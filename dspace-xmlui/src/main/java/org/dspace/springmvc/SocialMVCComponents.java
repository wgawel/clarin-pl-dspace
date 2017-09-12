package org.dspace.springmvc;

import org.dspace.app.xmlui.aspect.submission.submit.UploadStep;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.BeanNameViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Created by ondra on 4.5.17.
 *
 * Controller for connecting to providers.
 * @see DriveController for interaction with one of the providers.
 */
@Configuration
@Profile("drive-beta")
public class SocialMVCComponents {

    ConfigurationService configurationService = new DSpace().getConfigurationService();

    /**
     * This view resolver has the highest precedence to act before CocoonView and any other.
     * Uses the bean name to find the right view
     * @return
     */
    @Bean
    public ViewResolver beanViewResolver(){
        BeanNameViewResolver viewResolver = new BeanNameViewResolver();
        viewResolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return viewResolver;
    }

    /*
    @Bean(name={"connect/googleConnect", "connect/googleConnected", "connect/status"})
    public View googleConnectView() {
        return new GenericConnectionStatusView("google", "Google");
    }
    */

    /**
     * When a user connects to a provider he is redirected to 'connect/{provider}Connected'.
     * This view uses session attribute to redirect the user back to UploadStep.
     * With new providers adding new names to this bean should suffice.
     * @return
     */
    @Bean(name={"connect/googleConnected"})
    public View googleConnectView(){
        return new AbstractView() {
            @Override
            protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                String url = (String) request.getSession().getAttribute(UploadStep.RETURN_TO);
                response.sendRedirect(url);
            }
        };
    }

    /**
     * Controller that starts the OAuth dance. "Listens" on connect/{provider}. The provider must be configured
     * @see org.dspace.springsocial.SocialConfig for providers configuration
     * @param connectionFactoryLocator
     * @param connectionRepository
     * @return
     */
    @Bean
    public ConnectController connectController(ConnectionFactoryLocator connectionFactoryLocator, ConnectionRepository connectionRepository){
        ConnectController controller = new ConnectController(connectionFactoryLocator, connectionRepository);
        controller.setApplicationUrl(configurationService.getProperty("dspace.url"));
        return controller;
    }



}
