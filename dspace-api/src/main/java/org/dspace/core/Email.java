/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Class representing an e-mail message, also used to send e-mails.
 * <P>
 * Typical use:
 * <P>
 * <code>Email email = ConfigurationManager.getEmail(name);</code><br>
 * <code>email.addRecipient("foo@bar.com");</code><br>
 * <code>email.addArgument("John");</code><br>
 * <code>email.addArgument("On the Testing of DSpace");</code><br>
 * <code>email.send();</code><br>
 * <P>
 * <code>name</code> is the name of an email template in
 * <code>dspace-dir/config/emails/</code> (which also includes the subject.)
 * <code>arg0</code> and <code>arg1</code> are arguments to fill out the
 * message with.
 * <P>
 * Emails are formatted using <code>java.text.MessageFormat.</code>
 * Additionally, comment lines (starting with '#') are stripped, and if a line
 * starts with "Subject:" the text on the right of the colon is used for the
 * subject line. For example:
 * <P>
 *
 * <pre>
 *
 *     # This is a comment line which is stripped
 *     #
 *     # Parameters:   {0}  is a person's name
 *     #               {1}  is the name of a submission
 *     #
 *     Subject: Example e-mail
 *
 *     Dear {0},
 *
 *     Thank you for sending us your submission &quot;{1}&quot;.
 *
 * </pre>
 *
 * <P>
 * If the example code above was used to send this mail, the resulting mail
 * would have the subject <code>Example e-mail</code> and the body would be:
 * <P>
 *
 * <pre>
 *
 *
 *     Dear John,
 *
 *     Thank you for sending us your submission &quot;On the Testing of DSpace&quot;.
 *
 * </pre>
 *
 * <P>
 * Note that parameters like <code>{0}</code> cannot be placed in the subject
 * of the e-mail; they won't get filled out.
 *
 *
 * @author Robert Tansley
 * @author Jim Downing - added attachment handling code
 * @author Adan Roman Ruiz at arvo.es - added inputstream attachment handling code
 * @version $Revision: 5844 $
 */
public class Email
{
    /** The content of the message */
    private String content;

    /** The subject of the message */
    private String subject;

    /** The arguments to fill out */
    private List<Object> arguments;

    /** The recipients */
    private List<String> recipients;

    /** Reply to field, if any */
    private String replyTo;

    private List<FileAttachment> attachments;
    private List<InputStreamAttachment> moreAttachments;

    /** The character set this message will be sent in */
    private String charset;

    private static final Logger log = Logger.getLogger(Email.class);

    private static final BurstDelayQueue queue;

    static {
        queue = new BurstDelayQueue(new DSpace().getConfigurationService().getPropertyAsType("lr.email.burst", 20));
    }

    private static long lastAlert = 0;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    static {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                DelayedEmail email;
                try {
                    while ((email = queue.take()) != null) {
                        try {
                            email.send();
                            log.debug("Sent email, queue size " + queue.size());
                        }catch (MessagingException | IOException e){
                            log.error(e.getMessage());
                        }
                    }
                }catch (InterruptedException e){
                    log.error(e.getMessage());
                }

            }
        });

        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                log.debug("Scheduled email queue check");
                ConfigurationService configurationService = new DSpace().getConfigurationService();
                if(queue.size() > configurationService.getPropertyAsType("lr.email.queue.alert", 50)) {
                    Email email = new Email();
                    String errors_to = configurationService.getProperty("lr.lr.errors.email");
                    if ( isNotBlank(errors_to))
                    {
                        email.setSubject( String.format("DSpace email queue") );
                        email.setContent(String.format("There are currently %s emails in the queue.", queue.size()));
                        email.setCharset( "UTF-8" );
                        email.addRecipient(errors_to);
                    }
                    try {
                        long timeElapsedSinceLastEmail;
                        synchronized (scheduledExecutor){
                            timeElapsedSinceLastEmail = System.currentTimeMillis() - lastAlert;
                        }
                        if(timeElapsedSinceLastEmail > TimeUnit.MINUTES.toMillis(30)) {
                            email.doSend();
                            synchronized (scheduledExecutor) {
                                lastAlert = System.currentTimeMillis();
                            }
                            log.debug("Alert email sent.");
                        }
                    } catch (MessagingException | IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Create a new email message.
     */
    public Email()
    {
        arguments = new ArrayList<Object>(50);
        recipients = new ArrayList<String>(50);
        attachments = new ArrayList<FileAttachment>(10);
        moreAttachments = new ArrayList<InputStreamAttachment>(10);
        subject = "";
        content = "";
        replyTo = null;
        charset = null;
    }

    /**
     * Add a recipient
     *
     * @param email
     *            the recipient's email address
     */
    public void addRecipient(String email)
    {
        recipients.add(email);
    }

    /**
     * Set the content of the message. Setting this "resets" the message
     * formatting -<code>addArgument</code> will start. Comments and any
     * "Subject:" line must be stripped.
     *
     * @param cnt
     *            the content of the message
     */
    public void setContent(String cnt)
    {
        content = cnt;
        arguments = new ArrayList<Object>();
    }

    /**
     * Set the subject of the message
     *
     * @param s
     *            the subject of the message
     */
    public void setSubject(String s)
    {
        subject = s;
    }

    /**
     * Set the reply-to email address
     *
     * @param email
     *            the reply-to email address
     */
    public void setReplyTo(String email)
    {
        replyTo = email;
    }

    /**
     * Fill out the next argument in the template
     *
     * @param arg
     *            the value for the next argument
     */
    public void addArgument(Object arg)
    {
        arguments.add(arg);
    }

    public void addAttachment(File f, String name)
    {
        attachments.add(new FileAttachment(f, name));
    }
    public void addAttachment(InputStream is, String name,String mimetype)
    {
        moreAttachments.add(new InputStreamAttachment(is, name,mimetype));
    }

    public void setCharset(String cs)
    {
        charset = cs;
    }

    /**
     * "Reset" the message. Clears the arguments and recipients, but leaves the
     * subject and content intact.
     */
    public void reset()
    {
        arguments = new ArrayList<Object>(50);
        recipients = new ArrayList<String>(50);
        attachments = new ArrayList<FileAttachment>(10);
        moreAttachments = new ArrayList<InputStreamAttachment>(10);
        replyTo = null;
        charset = null;
    }

    /**
     * Sends the email.
     */
    public void send() throws MessagingException, IOException{
        long confDelay = new DSpace().getConfigurationService().getPropertyAsType("lr.email.delay", 30_000L);
        long delay =  confDelay * (queue.size() + 1);
        log.debug("Delay " + delay + ", queue size " + queue.size());
        queue.add(new DelayedEmail(this, delay));
    }

    /**
     * Sends the email.
     *
     * @throws MessagingException
     *             if there was a problem sending the mail.
     * @throws IOException
     */
    private void doSend() throws MessagingException, IOException
    {
        // Get the mail configuration properties
        String server = ConfigurationManager.getProperty("mail.server");
        String from = ConfigurationManager.getProperty("mail.from.address");
        boolean disabled = ConfigurationManager.getBooleanProperty("mail.server.disabled", false);

		// Set up properties for mail session
        Properties props = System.getProperties();
        props.put("mail.smtp.host", server);

        // Set the port number for the mail server
        String portNo = ConfigurationManager.getProperty("mail.server.port");
        if (portNo == null)
        {
        	portNo = "25";
        }
        props.put("mail.smtp.port", portNo.trim());
        // If no character set specified, attempt to retrieve a default
        if (charset == null)
        {
            charset = ConfigurationManager.getProperty("mail.charset");
        }

        // Get session
        Session session;
        
        // Get the SMTP server authentication information
        String username = ConfigurationManager.getProperty("mail.server.username");
        String password = ConfigurationManager.getProperty("mail.server.password");
        
        if (username != null)
        {
            props.put("mail.smtp.auth", "true");
            SMTPAuthenticator smtpAuthenticator = new SMTPAuthenticator(
                    username, password);
            session = Session.getDefaultInstance(props, smtpAuthenticator);
        }
        else
        {
            session = Session.getDefaultInstance(props);
        }

        // Set extra configuration properties
        String extras = ConfigurationManager.getProperty("mail.extraproperties");
        if ((extras != null) && (!"".equals(extras.trim())))
        {
            String arguments[] = extras.split(",");
            String key, value;
            for (String argument : arguments)
            {
                key = argument.substring(0, argument.indexOf('=')).trim();
                value = argument.substring(argument.indexOf('=') + 1).trim();
                props.put(key, value);
            }
        }

        // Create message
        MimeMessage message = new MimeMessage(session);

        // Set the recipients of the message
        Iterator<String> i = recipients.iterator();

        while (i.hasNext())
        {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(
                    i.next()));
        }

        // Format the mail message
        Object[] args = arguments.toArray();
        String fullMessage = MessageFormat.format(content, args);
        Date date = new Date();

        message.setSentDate(date);
        message.setFrom(new InternetAddress(from));

        // Set the subject of the email (may contain parameters)
        String fullSubject = MessageFormat.format(subject, args);
        if (charset != null)
        {
            message.setSubject(fullSubject, charset);
        }
        else
        {
            message.setSubject(fullSubject);
        }

        // Add attachments
        if (attachments.isEmpty() && moreAttachments.isEmpty())
        {
            // If a character set has been specified, or a default exists
            if (charset != null)
            {
                message.setText(fullMessage, charset);
            }
            else
            {
                message.setText(fullMessage);
            }
        }
        else{
        	 Multipart multipart = new MimeMultipart();
	            // create the first part of the email
	            BodyPart messageBodyPart = new MimeBodyPart();
	            messageBodyPart.setText(fullMessage);
	            multipart.addBodyPart(messageBodyPart);
        	 if(!attachments.isEmpty()){
	            for (Iterator<FileAttachment> iter = attachments.iterator(); iter.hasNext();)
	            {
	                FileAttachment f = iter.next();
	                // add the file
	                messageBodyPart = new MimeBodyPart();
	                messageBodyPart.setDataHandler(new DataHandler(
	                        new FileDataSource(f.file)));
	                messageBodyPart.setFileName(f.name);
	                multipart.addBodyPart(messageBodyPart);
	            }
	            message.setContent(multipart);
        	 }
        	 if(!moreAttachments.isEmpty()){
 	            for (Iterator<InputStreamAttachment> iter = moreAttachments.iterator(); iter.hasNext();)
 	            {
 	            	InputStreamAttachment isa = iter.next();
 	                // add the stream
 	                messageBodyPart = new MimeBodyPart();
 	                messageBodyPart.setDataHandler(new DataHandler(new InputStreamDataSource(isa.name,isa.mimetype,isa.is)));
 	                messageBodyPart.setFileName(isa.name);
 	                multipart.addBodyPart(messageBodyPart);
 	            }
 	            message.setContent(multipart);
        	 }
        }

        if (replyTo != null)
        {
            Address[] replyToAddr = new Address[1];
            replyToAddr[0] = new InternetAddress(replyTo);
            message.setReplyTo(replyToAddr);
        }

        if (disabled)
        {
            StringBuffer text = new StringBuffer(
                    "Message not sent due to mail.server.disabled:\n");

            Enumeration<String> headers = message.getAllHeaderLines();
            while (headers.hasMoreElements())
                text.append(headers.nextElement()).append('\n');

            if (!attachments.isEmpty())
            {
                text.append("\nAttachments:\n");
                for (FileAttachment f : attachments)
                    text.append(f.name).append('\n');
                text.append('\n');
            }

            text.append('\n').append(fullMessage);

            log.info(text);
        }
        else{
              try{
	        	Transport.send(message);
	        	StringBuffer mailTo = new StringBuffer();
	        	for(Address address : message.getAllRecipients()){
	        		mailTo.append(address).append(";");
	            }
        	    log.info(String.format("Email with subject %s successfully sent to the following people: %s", message.getSubject(),mailTo.toString()));
        	  }catch(MessagingException e){
        		log.error("Error while sending an email" + e.toString());
        		throw e;
        	  }  
		}
    }

    /**
     * Get the template for an email message. The message is suitable for
     * inserting values using <code>java.text.MessageFormat</code>.
     *
     * @param emailFile
     *            full name for the email template, for example "/dspace/config/emails/register".
     *
     * @return the email object, with the content and subject filled out from
     *         the template
     *
     * @throws IOException
     *             if the template couldn't be found, or there was some other
     *             error reading the template
     */
    public static Email getEmail(String emailFile)
            throws IOException
    {
        String charset = null;
        String subject = "";
        StringBuilder contentBuffer = new StringBuilder();
        InputStream is = null;
        InputStreamReader ir = null;
        BufferedReader reader = null;
        try
        {
            is = new FileInputStream(emailFile);
            ir = new InputStreamReader(is, "UTF-8");
            reader = new BufferedReader(ir);
            boolean more = true;
            while (more)
            {
                String line = reader.readLine();
                if (line == null)
                {
                    more = false;
                }
                else if (line.toLowerCase().startsWith("subject:"))
                {
                    subject = line.substring(8).trim();
                }
                else if (line.toLowerCase().startsWith("charset:"))
                {
                    charset = line.substring(8).trim();
                }
                else if (!line.startsWith("#"))
                {
                    contentBuffer.append(line);
                    contentBuffer.append("\n");
                }
            }
        } finally
        {
            if (reader != null)
            {
                try {
                    reader.close();
                } catch (IOException ioe)
                {
                }
            }
            if (ir != null)
            {
                try {
                    ir.close();
                } catch (IOException ioe)
                {
                }
            }
            if (is != null)
            {
                try {
                    is.close();
                } catch (IOException ioe)
                {
                }
            }
        }
        Email email = new Email();
        email.setSubject(subject);
        email.setContent(contentBuffer.toString());
        if (charset != null)
        {
            email.setCharset(charset);
        }
        return email;
    }
    /*
     * Implementation note: It might be necessary to add a quick utility method
     * like "send(to, subject, message)". We'll see how far we get without it -
     * having all emails as templates in the config allows customisation and
     * internationalisation.
     *
     * Note that everything is stored and the run in send() so that only send()
     * throws a MessagingException.
     */

    /**
     * Test method to send an email to check email server settings
     *
     * @param args Command line options
     */
    public static void main(String[] args)
    {
        String to = ConfigurationManager.getProperty("mail.admin");
        String subject = "DSpace test email";
        String server = ConfigurationManager.getProperty("mail.server");
        String url = ConfigurationManager.getProperty("dspace.url");
        Email e = new Email();
        e.setSubject(subject);
        e.addRecipient(to);
        e.content = "This is a test email sent from DSpace: " + url;
        System.out.println("\nAbout to send test email:");
        System.out.println(" - To: " + to);
        System.out.println(" - Subject: " + subject);
        System.out.println(" - Server: " + server);
        boolean disabled = ConfigurationManager.getBooleanProperty("mail.server.disabled", false);
        try
        {
            if( disabled)
            {
                System.err.println("\nError sending email:");
                System.err.println(" - Error: cannot test email because mail.server.disabled is set to true");
                System.err.println("\nPlease see the DSpace documentation for assistance.\n");
                System.err.println("\n");
                System.exit(1);
                return;
            }
            e.send();
        }
        catch (MessagingException me)
        {
            System.err.println("\nError sending email:");
            System.err.println(" - Error: " + me);
            System.err.println("\nPlease see the DSpace documentation for assistance.\n");
            System.err.println("\n");
            System.exit(1);
        }catch (IOException e1) {
        	System.err.println("\nError sending email:");
            System.err.println(" - Error: " + e1);
            System.err.println("\nPlease see the DSpace documentation for assistance.\n");
            System.err.println("\n");
            System.exit(1);
        }
        System.out.println("\nEmail sent successfully!\n");
    }

    /**
     * Use for tests only; hack around static initialization
     */
    public static void cleanup(int burstSize){
        queue.clear();
        queue.setBurstSize(burstSize);
    }

    /**
     * Utility struct class for handling file attachments.
     *
     * @author ojd20
     *
     */
    private static class FileAttachment
    {
        public FileAttachment(File f, String n)
        {
            this.file = f;
            this.name = n;
        }

        File file;

        String name;
    }

    /**
     * Utility struct class for handling file attachments.
     * 
     * @author Adán Román Ruiz at arvo.es
     * 
     */
    private static class InputStreamAttachment
    {
        public InputStreamAttachment(InputStream is, String name, String mimetype)
        {
            this.is = is;
            this.name = name;
            this.mimetype = mimetype;
        }

        InputStream is;
        String mimetype;
        String name;
    }

    private static class BurstDelayQueue extends DelayQueue<DelayedEmail> {

        private boolean burst = true;
        private int burstSize = 0;

        private final transient ReentrantLock myLock = new ReentrantLock();

        public BurstDelayQueue(int burstSize){
            super();
            this.burstSize = burstSize;
        }

        @Override
        public boolean add(DelayedEmail e){
            final ReentrantLock myLock = this.myLock;
            myLock.lock();
            try {
                if(burst && this.size() + 1 > burstSize){
                    burst = false;
                }
                if (burst) {
                    try {
                        e.send();
                        e = new DelayedEmail(null, e.getDelay(TimeUnit.MILLISECONDS));
                    } catch (IOException | MessagingException ex) {
                        log.error(ex.getMessage());
                    }
                }
            }finally {
                myLock.unlock();
            }
            return super.add(e);
        }

        @Override
        public DelayedEmail take() throws InterruptedException{
            final ReentrantLock lock = this.myLock;
            lock.lockInterruptibly();
            try {
                if (this.size() == 1) {
                    burst = true;
                }
            } finally {
                lock.unlock();
            }
            return super.take();
        }

        @Override
        public void clear(){
            final ReentrantLock lock = this.myLock;
            lock.lock();
            try {
                burst = true;
            } finally {
                lock.unlock();
            }
            super.clear();
        }

        /**
         * For tests only
         * @param burstSize
         */
        private void setBurstSize(int burstSize){
            this.burstSize = burstSize;
        }

    }

    /**
    *
    * @author arnaldo
    */
   public class InputStreamDataSource implements DataSource {
       private String name;       
       private String contentType;        
       private ByteArrayOutputStream baos;                
       
       InputStreamDataSource(String name, String contentType, InputStream inputStream) throws IOException {            
           this.name = name;            
           this.contentType = contentType;                        
           baos = new ByteArrayOutputStream();                        
           int read;            
           byte[] buff = new byte[256];            
           while((read = inputStream.read(buff)) != -1) {                
               baos.write(buff, 0, read);            
           }        
       }                
       
       public String getContentType() {            
           return contentType;        
       }         
       
       public InputStream getInputStream() throws IOException {            
           return new ByteArrayInputStream(baos.toByteArray());        
       }         
       
       public String getName() {            
           return name;        
       }         
       
       public OutputStream getOutputStream() throws IOException {            
           throw new IOException("Cannot write to this read-only resource");        
       }
   }

	/**
     * Inner Class for SMTP authentication information
     */
    private static class SMTPAuthenticator extends Authenticator
    {
        // User name
        private String name;
        
        // Password
        private String password;
        
        public SMTPAuthenticator(String n, String p)
        {
            name = n;
            password = p;
        }
        
        protected PasswordAuthentication getPasswordAuthentication()
        {
            return new PasswordAuthentication(name, password);
        }
    }

    private static class DelayedEmail implements Delayed{

        private Email email;
        private long startTime;

        DelayedEmail(Email email, long delayInMillis){
            this.email = email;
            this.startTime = System.currentTimeMillis() + delayInMillis;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = startTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(startTime, ((DelayedEmail)o).startTime);
        }

        public void send() throws IOException, MessagingException {
            if(email != null) {
                email.doSend();
            }
        }

    }

}
