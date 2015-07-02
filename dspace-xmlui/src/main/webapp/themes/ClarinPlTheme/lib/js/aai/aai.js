'use strict';
(function(window){
  function AAI() {
    var host = 'https://' + window.location.hostname,
        ourEntityID = host.match("clarin-pl.eu") ? "http://www.clarin-pl.eu/dspace" : host;
    this.defaults = {
      //host : 'https://clarin-pl.eu',
      host : host, //better default (useful when testing on ufal-point-dev)
      // do not add protocol because an error will appear in the DJ dialog
      // if you see the error, your SP is not listed among djc trusted (edugain is enough to be trusted)
      responseUrl: window.location.protocol + '//clarin-pl.eu/dspace/themes/ClarinPlTheme/lib/js/aai/discojuiceDiscoveryResponse.html',
      ourEntityID: ourEntityID + '/shibboleth/eduid/sp',
      serviceName: '',
      metadataFeed: host + '/dspace/discojuice/feeds',
      selector: 'a.signon', // selector for login button
      autoInitialize: true // auto attach DiscoJuice to DOM
    };
    this.setup = function(options) {
      var opts = jQuery.extend({}, this.defaults, options),
          defaultCallback = function(e) {
            window.location = opts.host + '/shibboleth/Shibboleth.sso/Login?SAMLDS=1&target=' + opts.target + '&entityID=' + window.encodeURIComponent(e.entityID);
          };
      //console.log(opts);
      if(!opts.target){
        throw 'You need to set the \'target\' parameter.';
      }
      // call disco juice setup
      if (!opts.autoInitialize || $(opts.selector).size() > 0) {
        if(! window.DiscoJuice ){
          throw 'Failed to find DiscoJuice. Did you include all that is necessary?';
        }
        var djc = DiscoJuice.Hosted.getConfig(
          opts.serviceName,
          opts.ourEntityID,
          opts.responseUrl,
          [ ],
          opts.host + '/shibboleth/Shibboleth.sso/DS?SAMLDS=1&target='+opts.target+'&entityID=');
        djc.metadata = [opts.metadataFeed];
        djc.subtitle = "Login via Your home institution (e.g. university)";
	djc.textHelp = opts.textHelp;
	djc.textHelpMore = opts.textHelpMore;

        djc.inlinemetadata = typeof opts.inlinemetadata === 'object' ? opts.inlinemetadata : [];
        djc.inlinemetadata.push({
          'country': '_all_',
          'entityID': 'https://idp.clarin.eu',
          'geo': {'lat': 51.833298, 'lon': 5.866699},
          'title': 'Clarin.eu website account',
          'weight': 1000
        });

        if(opts.localauth) {

          djc.inlinemetadata.push(
            {
              'entityID': 'local://',
              'auth': 'local',
              'title': 'Local authentication',
              'country': '_all_',
              'geo': null,
              'weight': 1000
            });

          djc.inlinemetadata.push({
              'country': 'PL',
              'entityID': 'https://login.aai.pionier.net.pl/IdP/saml2/idp/metadata.php',
              'geo': {'lat': 52.4085, 'lon': 16.934278},
              'title': 'PIONIER',
              'weight': 700
           });

          djc.callback = function(e){
            var auth = e.auth || null;
            switch(auth) {
              case 'local':
                DiscoJuice.UI.setScreen(opts.localauth);
                jQuery('input#login').focus();
                break;
              //case 'saml':
              default:
                defaultCallback(e);
                break;
            }
          };
        }

        if (opts.callback && typeof opts.callback === 'function') {
          djc.callback = function(e) {
            opts.callback(e, opts, defaultCallback);
          };
        }

        if (opts.autoInitialize) {
          jQuery(opts.selector).DiscoJuice( djc );
        }

        return djc;
      } //if jQuery(selector)
    };
  }

  if (!window.aai) {
    window.aai = new AAI();
  }
})(window);
