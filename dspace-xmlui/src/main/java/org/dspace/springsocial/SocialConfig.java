package org.dspace.springsocial;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.utils.DSpace;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurer;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.jdbc.JdbcUsersConnectionRepository;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.connect.GoogleConnectionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import static org.springframework.security.crypto.util.EncodingUtils.concatenate;
import static org.springframework.security.crypto.util.EncodingUtils.subArray;

/**
 * Created by ondra on 21.4.17.
 *
 * Configuration of spring-social application wide bits and pieces.
 * included in application context dspace-xmlui/src/main/webapp/WEB-INF/spring/applicationContext.xml
 * with component-scan on org.dspace.springsocial
 * @see org.dspace.springmvc.SocialMVCComponents for mvc/servlet context configuration
 */
@Configuration
@EnableSocial
@EnableTransactionManagement
@EnableAsync
@Profile("drive-beta")
public class SocialConfig implements SocialConfigurer, AsyncConfigurer {

    Logger log = Logger.getLogger(SocialConfig.class);

    ConfigurationService configurationService = new DSpace().getConfigurationService();

    /**
     * Social providers are configured here.
     * @param connectionFactoryConfigurer
     * @param environment
     */
    @Override
    public void addConnectionFactories(ConnectionFactoryConfigurer connectionFactoryConfigurer, Environment environment) {
        String clientId = configurationService.getProperty("social.google.clientId");
        String clientSecret = configurationService.getProperty("social.google.clientSecret");
        connectionFactoryConfigurer.addConnectionFactory(new GoogleConnectionFactory(clientId, clientSecret));
    }

    /**
     * We need a user id to store connections to various providers.
     * Id of the currently logged eperson is used. It's a String!
     * @return
     */
    @Override
    public UserIdSource getUserIdSource() {
        return new UserIdSource() {
            @Override
            public String getUserId() {
                try {
                    Context context = ContextUtil.obtainContext(new DSpace().getRequestService().getCurrentRequest().getHttpServletRequest());
                    EPerson ePerson = context.getCurrentUser();
                    if(ePerson != null){
                        return Integer.toString(ePerson.getID());
                    }
                }catch (SQLException e){
                    log.error(e);
                }
                throw new IllegalStateException("Unable to get a ConnectionRepository: no user signed in");
            }
        };
    }

    /**
     * Use the dspace database it's connection pool etc.
     * @return
     */
    @Bean
    public DataSource dataSource(){
        return DatabaseManager.getDataSource();
    }

    /**
     * Without TransactionManager and @EnableTransactionManagement JdbcConnectionRepository, which uses @Transactional,
     * was not committing to the database.
     * @return
     */
    @Bean
    public PlatformTransactionManager txManager() {
          return new DataSourceTransactionManager(dataSource());
    }

    /**
     * Database backed connection repository (storage mainly for oauth tokens). The tokens are encrypted.
     * Without JCE aes-128 is the strongest you can get.
     * @param connectionFactoryLocator
     * @return
     */
    @Override
    public UsersConnectionRepository getUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator) {
        String password = configurationService.getProperty("social.userconnection.encrypt.password");
        String salt = configurationService.getProperty("social.userconnection.encrypt.salt");
        return new JdbcUsersConnectionRepository(dataSource(), connectionFactoryLocator,
                NoJCEEncryptors.queryableText(password, salt));
    }

    /**
     * Request scoped connection, ie. belonging to the current user, to Google. Used from DriveController, initiated
     * from UploadStep if not present.
     * @param connectionRepository
     * @return
     */
    @Bean
    @Scope(value="request", proxyMode = ScopedProxyMode.INTERFACES)
    public Google google(ConnectionRepository connectionRepository){
        Connection<Google> googleConnection = connectionRepository.getPrimaryConnection(Google.class);
        if(googleConnection.hasExpired()){
            googleConnection.refresh();
        }
        return googleConnection.getApi();
    }

    /**
     * Class that fetches files in the background and adds them to an item.
     * @param usersConnectionRepository
     * @return
     */
    @Bean
    public AsyncBitstreamAdder asyncBitstreamAdder(UsersConnectionRepository usersConnectionRepository){
        log.debug("===SocialConfig.asyncBitstreamAdder");
        return new AsyncBitstreamAdder(usersConnectionRepository);
    }

    /**
     * ThreadPool for AsyncBitstreamAdder
     * TODO use dspace/tomcat wide thread pool?
     * @return
     */
    @Override
    public Executor getAsyncExecutor() {
        log.debug("===SocialConfig.getAsyncExecutor");
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //start a new thread if less then coreSize threads are running
        executor.setCorePoolSize(cores*2);
        //start a new thread if > coreSize & < maxSize & queue is full
        executor.setMaxPoolSize(cores*4);
        executor.setQueueCapacity(cores);
        executor.setThreadNamePrefix("MyAsyncExecutor-");
        executor.initialize();
        return executor;
    }

    /**
     * Encryptors that should work without JCE, ie. using weaker crypto. Changing the key length from 256 to 128
     * is the only change to the code from following classes.
     * @see org.springframework.security.crypto.encrypt.Encryptors
     * @see org.springframework.security.crypto.encrypt.AesBytesEncryptor
     * @see org.springframework.security.crypto.encrypt.CipherUtils
     */
    private static class NoJCEEncryptors {

        /**
         * Creates an encryptor for queryable text strings that uses standard password-based
         * encryption. Uses a 16-byte all-zero initialization vector so encrypting the same
         * data results in the same encryption result. This is done to allow encrypted data to
         * be queried against. Encrypted text is hex-encoded.
         *
         * @param password the password used to generate the encryptor's secret key; should
         *                 not be shared
         * @param salt     a hex-encoded, random, site-global salt value to use to generate the
         *                 secret key
         */
        public static TextEncryptor queryableText(CharSequence password, CharSequence salt) {
            return new HexEncodingTextEncryptor(new AesBytesEncryptor(password.toString(),
                    salt));
        }

        /**
         * Delegates to an {@link BytesEncryptor} to encrypt text strings.
         * Raw text strings are UTF-8 encoded before being passed to the encryptor.
         * Encrypted strings are returned hex-encoded.
         * @author Keith Donald
         */
        final static class HexEncodingTextEncryptor implements TextEncryptor {

            private final BytesEncryptor encryptor;

            public HexEncodingTextEncryptor(BytesEncryptor encryptor) {
                this.encryptor = encryptor;
            }

            public String encrypt(String text) {
                return new String(Hex.encode(encryptor.encrypt(Utf8.encode(text))));
            }

            public String decrypt(String encryptedText) {
                return Utf8.decode(encryptor.decrypt(Hex.decode(encryptedText)));
            }

        }

        /**
         * Encryptor that uses 128-bit AES encryption. Original version in spring-security-crypto uses 256
         *
         * @author Keith Donald
         * @author Dave Syer
         */
        final static class AesBytesEncryptor implements BytesEncryptor {

            private final SecretKey secretKey;

            private final Cipher encryptor;

            private final Cipher decryptor;

            private final BytesKeyGenerator ivGenerator;

            private CipherAlgorithm alg;

            private static final String AES_CBC_ALGORITHM = "AES/CBC/PKCS5Padding";

            private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";

            public enum CipherAlgorithm {

                CBC(AES_CBC_ALGORITHM, NULL_IV_GENERATOR), GCM(AES_GCM_ALGORITHM, KeyGenerators
                        .secureRandom(16));

                private BytesKeyGenerator ivGenerator;
                private String name;

                private CipherAlgorithm(String name, BytesKeyGenerator ivGenerator) {
                    this.name = name;
                    this.ivGenerator = ivGenerator;
                }

                @Override
                public String toString() {
                    return this.name;
                }

                public AlgorithmParameterSpec getParameterSpec(byte[] iv) {
                    return this == CBC ? new IvParameterSpec(iv) : new GCMParameterSpec(128, iv);
                }

                public Cipher createCipher() {
                    return newCipher(this.toString());
                }

                public BytesKeyGenerator defaultIvGenerator() {
                    return this.ivGenerator;
                }
            }

            public AesBytesEncryptor(String password, CharSequence salt) {
                this(password, salt, null);
            }

            public AesBytesEncryptor(String password, CharSequence salt,
                                     BytesKeyGenerator ivGenerator) {
                this(password, salt, ivGenerator, CipherAlgorithm.CBC);
            }

            public AesBytesEncryptor(String password, CharSequence salt,
                                     BytesKeyGenerator ivGenerator, CipherAlgorithm alg) {
                PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), Hex.decode(salt),
                        1024, 128);
                SecretKey secretKey = newSecretKey("PBKDF2WithHmacSHA1", keySpec);
                this.secretKey = new SecretKeySpec(secretKey.getEncoded(), "AES");
                this.alg = alg;
                this.encryptor = alg.createCipher();
                this.decryptor = alg.createCipher();
                this.ivGenerator = ivGenerator != null ? ivGenerator : alg.defaultIvGenerator();
            }

            public byte[] encrypt(byte[] bytes) {
                synchronized (this.encryptor) {
                    byte[] iv = this.ivGenerator.generateKey();
                    initCipher(this.encryptor, Cipher.ENCRYPT_MODE, this.secretKey,
                            this.alg.getParameterSpec(iv));
                    byte[] encrypted = doFinal(this.encryptor, bytes);
                    return this.ivGenerator != NULL_IV_GENERATOR ? concatenate(iv, encrypted)
                            : encrypted;
                }
            }

            public byte[] decrypt(byte[] encryptedBytes) {
                synchronized (this.decryptor) {
                    byte[] iv = iv(encryptedBytes);
                    initCipher(this.decryptor, Cipher.DECRYPT_MODE, this.secretKey,
                            this.alg.getParameterSpec(iv));
                    return doFinal(
                            this.decryptor,
                            this.ivGenerator != NULL_IV_GENERATOR ? encrypted(encryptedBytes,
                                    iv.length) : encryptedBytes);
                }
            }

            // internal helpers

            private byte[] iv(byte[] encrypted) {
                return this.ivGenerator != NULL_IV_GENERATOR ? subArray(encrypted, 0,
                        this.ivGenerator.getKeyLength()) : NULL_IV_GENERATOR.generateKey();
            }

            private byte[] encrypted(byte[] encryptedBytes, int ivLength) {
                return subArray(encryptedBytes, ivLength, encryptedBytes.length);
            }

            private static final BytesKeyGenerator NULL_IV_GENERATOR = new BytesKeyGenerator() {

                private final byte[] VALUE = new byte[16];

                public int getKeyLength() {
                    return this.VALUE.length;
                }

                public byte[] generateKey() {
                    return this.VALUE;
                }

            };

            /**
             * Constructs a new Cipher.
             */
            public static Cipher newCipher(String algorithm) {
                try {
                    return Cipher.getInstance(algorithm);
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalArgumentException("Not a valid encryption algorithm", e);
                } catch (NoSuchPaddingException e) {
                    throw new IllegalStateException("Should not happen", e);
                }
            }

            /**
             * Invokes the Cipher to perform encryption or decryption (depending on the initialized mode).
             */
            public static byte[] doFinal(Cipher cipher, byte[] input) {
                try {
                    return cipher.doFinal(input);
                } catch (IllegalBlockSizeException e) {
                    throw new IllegalStateException("Unable to invoke Cipher due to illegal block size", e);
                } catch (BadPaddingException e) {
                    throw new IllegalStateException("Unable to invoke Cipher due to bad padding", e);
                }
            }

            /**
             * Initializes the Cipher for use.
             */
            public static void initCipher(Cipher cipher, int mode, SecretKey secretKey, AlgorithmParameterSpec parameterSpec) {
                try {
                    if (parameterSpec != null) {
                        cipher.init(mode, secretKey, parameterSpec);
                    } else {
                        cipher.init(mode, secretKey);
                    }
                } catch (InvalidKeyException e) {
                    throw new IllegalArgumentException("Unable to initialize due to invalid secret key", e);
                } catch (InvalidAlgorithmParameterException e) {
                    throw new IllegalStateException("Unable to initialize due to invalid decryption parameter spec", e);
                }
            }

            /**
             * Generates a SecretKey.
             */
            public static SecretKey newSecretKey(String algorithm, PBEKeySpec keySpec) {
                try {
                    SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
                    return factory.generateSecret(keySpec);
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalArgumentException("Not a valid encryption algorithm", e);
                } catch (InvalidKeySpecException e) {
                    throw new IllegalArgumentException("Not a valid secret key", e);
                }
            }
        }


    }

}
