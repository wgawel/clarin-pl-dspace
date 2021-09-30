jQuery(document).ready(function() {
	
	var getUrlParameter = function getUrlParameter(sParam) {
		var sPageURL = decodeURIComponent(window.location.search
				.substring(1)), sURLVariables = sPageURL
				.split('&'), sParameterName, i;

		for (i = 0; i < sURLVariables.length; i++) {
			sParameterName = sURLVariables[i].split('=');
	
			if (sParameterName[0] === sParam) {
				return sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]).replace(/</g, "&lt;").replace(/>/g, "&gt;");
			}
		}
	};

	var aagregatorUrl = "https://clarin-aa.ms.mff.cuni.cz/aaggreg/v1/";
	var idpDetail = aagregatorUrl + "entity/?entityID=";
	var idpEntityId = getUrlParameter("idpEntityId");
	var ourEntityId = getUrlParameter("ourEntityId");
	var cc = getUrlParameter("cc");

	var error = function() {
		jQuery(".aai-error").each(function() {
			jQuery("#loading").hide();
			var div = jQuery(this);
			var heading = jQuery(".error-heading", div);
			var details = jQuery(".error-details", div);
			heading.html('<i class="fa fa-warning fa-lg ">&#160;</i>' + jQuery.i18n._('login-error'));
			details.html('<div class="text-center" style="font-size: 130%;">' + jQuery.i18n._('login-missing-info-send-us', idpEntityId) + '<br><a class="btn btn-sm btn-danger" style="margin: 10px;" href="' + jQuery('.helpdesk').attr('href') + '">' + jQuery.i18n._('login-contact-helpdesk') + '</a></div>');
		});
	};
	
	


	// email is prepared in english only - no reason to localize
	// (what is the support@idp locale)
	var messageBody = 'Dear all,\n'
		+ 'While accessing a service identified by ' + ourEntityId
		+ ' I received an error saying my identity provider ' + idpEntityId
		+ ' does not release all the necessary attributes.'
		+ ' The SP implements data protection code of conduct'
		+ ' (http://geant3plus.archive.geant.net/uri/dataprotection-code-of-conduct/V1/Pages/default.aspx),'
		+ ' is a member of the REFEDS Research and Scholarship Entity Category and a member of the CLARIN infrastructure'
		+ ' (http://clarin.eu and https://www.clarin.eu/content/service-provider-federation).\n'

	var subject = 'Attributes not released by ' + idpEntityId;

	jQuery(".aai-error").each(function() {
		
		var div = jQuery(this);
		
		if (idpEntityId && ourEntityId) {
			jQuery
				.when(jQuery.getJSON(idpDetail + idpEntityId))
				.done(
					function(data) {
						if (!data.ok || data.result.length < 1) {
							error();
							return;
						}
						
						var contact = data.result[0].email_support || data.result[0].email_administrative || data.result[0].email_technical;
						var idp_display_name = data.result[0]["displayName_en"] || idpEntityId;

						if (contact && messageBody && subject && cc && ourEntityId) {
							
							var heading = jQuery(".error-heading", div);
							var details = jQuery(".error-details", div);

							var href =  "mailto:" + contact + "?" + "cc=" + cc +
							"&subject=" + encodeURIComponent(subject) + "&body=" + encodeURIComponent(messageBody.replace("\n", "\n\r"));	
							
							heading.html('<i class="fa fa-warning fa-lg ">&#160;</i>' + jQuery.i18n._('login-cannot-continue'));
							details.append('<div class="text-center" style="font-size: 130%;">' + 
							jQuery.i18n._('login-missing-info-send-you', idp_display_name) +
							'</div>');
							
							var mailContainer = jQuery('<dl />');
							mailContainer.append('<div class="text-center"><a class="btn btn-sm btn-danger bold" style="margin: 10px;" href=' + href + '><i class="fa fa-envelope">&#160;</i> ' + jQuery.i18n._('login-email-send') + '</a></div>');	

							// subject, body and cc contain user params, don't
							// execute them in jQuery() or append() insert them using text()!
														
							var toItem = jQuery("<dd style='font-size: 90%; padding: 5px;' />");
							toItem.text(contact + ', ' + cc);							
							mailContainer.append("<dt>" + jQuery.i18n._('login-email-to') + "</dt>");
							mailContainer.append(toItem);

							var subjItem = jQuery("<dd style='font-size: 90%; padding: 5px;' />");
							subjItem.text(subject);
							mailContainer.append("<dt>" + jQuery.i18n._('login-email-subject') + "</dt>");
							mailContainer.append(subjItem);
							
							var bodyItem = jQuery("<dd style='font-size: 90%; padding: 5px; white-space: pre-wrap; display: block;' />");
							bodyItem.text(messageBody);
							mailContainer.append("<dt>" + jQuery.i18n._('login-email-body') + "</dt>");
							mailContainer.append(bodyItem);
														
							details.append("<div class='alert alert-danger'>" + mailContainer.html() + "</div>")
							
						} else {
							error();
							return;
						}
						jQuery("#loading").hide();
					}).fail(function() {
						error();
						return;
					});
				} else {
					error();
					return;
				}
			});
});
