/**
 * Created by ondra on 4.5.17.
 */
/*global jQuery */
/*global console */
/*jshint globalstrict: true*/
/*jshint multistr: true */
'use strict';
function convertBytesToHumanReadableForm(b) {
    var units = [ 'bytes', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB' ];
    var exp = b === 0 ? 0 : Math.round(Math.log(b) / Math.log(1024));
    return Math.round((b / Math.pow(1024, exp)) * 100) / 100 + " " + units[exp];
}

jQuery("a.turn_me_into_ajax_post_button").click(function (e) {
    e.preventDefault();
    var url_parts = this.href.split('?');
    var url = url_parts[0];
    var params = url_parts[1].split('&');
    var jForm = jQuery("<form>").attr('method', 'POST').attr('action', url);
    jQuery.each( params, function ( index, val ) {
        var parts = val.split('=');
        jForm.append(jQuery('<input type="hidden">').attr('name', parts[0]).val(decodeURI(parts[1])));
    });
    jQuery('body').append(jForm);
    jForm.submit();
});

var done_src = ufal.utils.get_dspace_url() + '/themes/UFAL/images/check_606060_15x15.png';
var proc_src = ufal.utils.get_dspace_url() + '/themes/UFAL/images/loading.gif';
var err_src = ufal.utils.get_dspace_url() + '/themes/UFAL/images/notok.jpg';

var modal_str = '<div class="modal fade" id="provided_files" tabindex="-1" role="dialog">\
<div class="modal-dialog">\
<div class="modal-content">\
<div class="modal-header">\
<button type="button" class="close" data-dismiss="modal">&times;</button>\
<h4 class="modal-title">' + jQuery.i18n._("Select your file") + '</h4>\
</div>\
<div class="modal-body" style="overflow-y: scroll" id="provided_files_modal_body">\
<div id="file_listing" />\
</div>\
<div class="modal-footer">\
<div id="loading-indicator"><img src="' + proc_src + '" />\
' + jQuery.i18n._("Fetching file list") + '</div>\
</div>\
</div>\
</div>';
var jModal = jQuery(modal_str);
var jModalBody = jModal.find("#provided_files_modal_body");
var jModalFileList = jModal.find("#file_listing");
var jModalFooter = jModal.find(".modal-footer");
jModalBody.css('max-height',jQuery(window).height()/4);
//prevent double scrollbars
jQuery('body').append(jModal);

var fetchedFiles = {};

/*
 * Show loading gif when fetching
*/
jQuery(document).ajaxSend(function(event, request, settings) {
    jQuery('#loading-indicator').show();
});

jQuery(document).ajaxComplete(function(event, request, settings) {
    jQuery('#loading-indicator').hide();
});

jQuery(".social-provider-list-url").click(function(e){
    e.preventDefault();
    var filesUrl = this.href;

    /*
    * Clicking on directory, fetches it's content, redraws and updates the trail with one more lvl 
    */
    var directoryClick = function(url, title, trail){
                    return function(e){
                            e.preventDefault();
                            trail.push({title:title, url:url});
                            jQuery.ajax(url).done(drawPage(trail));
                    };
    };

    /*
    * Add the items from a page add a scroll event listener if there are other pages,
    * add event listener on directories clicks
    */
    var processPage = function(trail, page){
            //first unbind previous scroll events, because the token needs to be changed
            jModalBody.unbind('scroll');
            if(page && page.items && page.items.length > 0) {
                page.items.forEach(function(item){
                    var jItemRow = jQuery("<li>").append(jQuery("<img>").attr("src",item.iconLink));
                    var jItemLink = jQuery("<a>").attr("href", filesUrl + "/" + item.id).append(item.title);
                    if(item.folder){
                        var url = jItemLink.attr("href");
                        jItemLink.click(directoryClick(url, item.title, trail));
                    }else{
                        jItemLink.click(function(e){
                            e.preventDefault();
                            //start downloading the file
                            jQuery.ajax({url: this.href, type: 'post'}).done(
                                function(data, status, xhr){
                                    //display processing image
                                    var jImg = jQuery("<img style='height: 15px; width: 15px;'>")
                                        .attr('src', proc_src);
                                    fetchedFiles[item.id] = {status_url: xhr.getResponseHeader('Location'),
                                                         status: "Processing", img: jImg, name: item.title};
                                    jItemRow.append(jImg);
                                });
                        });
                    }
                    jModalFileList.append(jItemRow.append(jItemLink).append("..." + convertBytesToHumanReadableForm(item.fileSize)));
                    //On redraw check the status + add an img + update the img ref for later updates
                    if(fetchedFiles[item.id]){
                        var file = fetchedFiles[item.id];
                        var img_src = file.status === "Done" ? done_src : proc_src;
                        var jImg = jQuery("<img style='height: 15px; width: 15px;'>")
                            .attr('src', img_src);
                        file.img = jImg;
                        jItemRow.append(jImg);
                    }
                });
            }else {
                jModalFileList.append(jQuery("<li>").append(jQuery.i18n._("No files or all files hidden")));
            }
            if(page.nextPageToken){
                jModalBody.scroll(function() {
                    //if scrollbar reaches the "bottom" of the list. "Bottom" is the actual list size minus
                    //the size of visible part
                    if(jModalBody.scrollTop() === jModalFileList.height() - jModalBody.height()) {
                        //add next page items
                        jQuery.ajax({url: trail[trail.length - 1].url, type: 'get', data: {pageToken: page.nextPageToken}})
                                .done(function(page){processPage(trail, page);});
                    }
                });
            }
    };

    /*
    * Draw or redraw filelist modal. Trail is the path through directory structure,
    * it contains titles and links
    */
    var drawPage = function(trail){
        jModalFileList.empty();
        jModalBody.unbind('scroll');
        for(var i = 0; i < trail.length; i++){
            var jTrailLink = jQuery("<a>").attr("href", trail[i].url).append(trail[i].title);
            jTrailLink.click(directoryClick(trail[i].url, trail[i].title, trail.slice(0, i)));
            jModalFileList.append(jTrailLink).append(' > ');
        }
        jModal.modal();
        return function (page) {
            processPage(trail, page);
        };
    };

    //actually fetch the root on click
    jQuery.ajax(filesUrl)
        .done(drawPage([{title:'/', url:filesUrl}]));

    //Periodically check for status of files
    setInterval(function(){
        Object.keys(fetchedFiles).forEach(function(id, index) {
            var file = fetchedFiles[id];
            if(file.status !== "Done") {
                jQuery.ajax(file.status_url).done(function (file_status) {
                    if (file_status === "Done") {
                        //change wheel to check mark
                        file.img.attr('src', done_src);
                        file.status = file_status;
                    } else if (file_status === "Processing") {
                        //keep the wheel
                    }
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    file.status = "Done";
                    file.img.attr('src', err_src);
                    window.alert(jQuery.i18n._("Could not add %s.\n%s\n%s\n%s", file.name, textStatus, errorThrown, jqXHR.responseText));
                });
            }
       });
    }, 5000);

    //on modal close, check progress and do a refresh
    jModal.on('hide.bs.modal',function() {
        var not_finished_names = [];

        Object.keys(fetchedFiles).forEach(function(id, index) {
            var file = fetchedFiles[id];
            if(file.status !== "Done"){
               not_finished_names.push(file.name);
            }
        });

        if(not_finished_names.length > 0){
            window.alert(jQuery.i18n._("The following files are still being processed, you won't be notified on the progress but they should eventually end up in the submission. " + not_finished_names.join(", ")));
        }

        //refresh by submitting
        var jForm = jQuery("form[action*='continue']");
        jForm.submit();
    });
    
    //TODO errors on ajax

});

