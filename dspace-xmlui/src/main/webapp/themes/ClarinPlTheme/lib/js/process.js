function startProcess(link, status){
	$( "#process-doc-button").hide();
	$( "#process-loading").show();
	$.ajax({
          url: link ,
          async: false
     });
    $( "#process-loading").hide();
	getProcessStatus(status);
}

function getProcessStatus(link){
  $.getJSON(link, function(j) {
    updateProcessStatus(j,link);
  });
}

function updateProcessStatus(s,link){
	if(s.status === 'READY'){
		$( "#process-doc-button").show();
	}
	if(s.status === 'PROCESSING'){
		$( "#process-doc-button").hide();
		$( "#export-to-inforex").hide();
		$( "#export-to-wielowyr").hide();
		$( "#process-progres" ).show();
		var progres = s.progress*100;
		$( "#p-progress" ).css('width', progres+'%').attr('aria-valuenow',progres);
		setTimeout(function () { getProcessStatus(link); }, 1000);
	}

	if(s.status === 'DONE'){
		$( "#export-to-inforex").show();
		$( "#export-to-wielowyr").show();
		$( "#goto-freq-lists").show();
		$( "#process-progres" ).hide();
		$( "#freq_token").attr("value","/public-dspace/"+s.handle+"/wlw_ccl.zip")
	}

	if(s.status === 'ERROR'){
		$( "#process-doc-button").show();
		$( "#process-progres" ).hide();
		$( "#process-msg" ).show();
		$( "#process-msg" ).text(s.error);
	}
}

function exportInforex(link,email){
	$( "#export-to-inforex" ).hide();
	$( "#inforex-loading" ).show();
	$.getJSON(link+"?userEmail="+email, function(j) {
		if( j.error !== null){
			$( "#process-msg" ).text("Inforex error message: "+ j.error);
    		$( "#inforex-loading").hide();
			$( "#process-msg" ).show();
    		$( "#export-to-inforex" ).show();
    	}
    	if(j.redirect !== null){
    		window.open(j.redirect, '_newtab');
    		$( "#inforex-loading").hide();
    		$( "#export-to-inforex" ).show();
    	}
	});
}

function exportMewex(link,email){
	$( "#export-to-wielowyr" ).hide();
	$( "#wielowyr-loading" ).show();
	$.getJSON(link+"?userEmail="+email, function(j) {
		if( j.error !== null){
			$( "#process-msg" ).show();
    		$( "#process-msg" ).text("MaWeX error message: "+ j.error);
    		$( "#export-to-wielowyr" ).show();
			$( "#wielowyr-loading" ).hide();
    	}
    	if(j.redirect !== null){
    		window.open(j.redirect, '_newtab');
    		$( "#wielowyr-loading" ).hide();
    		$( "#export-to-wielowyr" ).show();
    	}
	});
}