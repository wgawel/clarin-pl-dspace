var client = new XMLHttpRequest();

function upload(id,handle,bitstreamId) {
	
	var file = document.getElementById(id);
	var idTag = "#"+id;

	var uploadForm = new FormData();
	uploadForm.append("file", file.files[0]);
	uploadForm.append("handle", handle);
	uploadForm.append("bitstreamId", bitstreamId);


	$.ajax({
		url : '/dspace/api/cmdi/upload',
		data : uploadForm,
		dataType : 'text',
		processData : false,
		contentType : false,
		type : 'POST',
		success : function(data) {
			$(idTag).closest("td").prev().html(file.files[0].name);
		},
		error: function(data) {
			alert("Error");
		}
	});
}

function cloneElement(element){
	var removeBtn = document.createElement("button");
	removeBtn.type = "button";
	removeBtn.onclick = function() {
		this.parentNode.remove();
    };
	removeBtn.innerHTML="-";
	var node = element.parentNode;
	var cln = node.cloneNode(true);
	cln.getElementsByTagName("button")[0].remove();
	cln.insertBefore(removeBtn, cln.firstChild);
	node.parentNode.insertBefore(cln, node.nextSibling);
	
}

function cloneComponent(element){
	var removeBtn = document.createElement("button");
	removeBtn.type = "button";
	removeBtn.onclick = function() {
		this.parentNode.remove();
    };
	removeBtn.innerHTML="-";
	var node = element.parentNode;
	var cln = node.cloneNode(true);
	cln.getElementsByTagName("button")[0].remove();
	cln.insertBefore(removeBtn, cln.firstChild);
	node.parentNode.insertBefore(cln, node.nextSibling);
}

$.createElement = function(name){
    return $('<'+name+' />');
};

$.fn.appendNewElement = function(name)
{
    this.each(function(i)
    {
        $(this).append('<'+name+' />');
    });
    return this;
};

function firstChild(){
	var $form = $('#xmlForm');
	var $first = $(":first-child", $form);
	return $first.attr('id');
}

function rootElement(){
	return '<?xml version="1.0" encoding="utf-8" ?><'+ firstChild() +'/>';
};

function buildXml(){

	var $root = $('<XMLDocument />');
	var $form = $('#xmlForm');
	var $first = $(":first-child", $form);
	
	var topId = $first.attr('id');
	$root.appendNewElement(topId);
	
	var $xml = $root.find(topId);
	make($xml, $first);
	
	function make($ref, $source){
		$source.children("div").each(function(){
			var $ele = $(this);
			var $newComponent = $.createElement($ele.attr('id'));
			if($ele.hasClass('element')){
				var input = $ele.find('input').val();
				var select = $ele.find('select').val();
				if(select !=''){
					$newComponent.text(select);
				}
				if(input !=''){
					$newComponent.text(input);
				}
			}
			if(this.children.length > 0){
				make($newComponent, $ele);				
			};
			$ref.append($newComponent);
		});
	} 
	console.log($root.html());
}

function cloneElement(element){
	var removeBtn = document.createElement("button");
	removeBtn.type = "button";
	removeBtn.onclick = function() {
		this.parentNode.remove();
    };
	removeBtn.innerHTML="-";
	var node = element.parentNode;
	var cln = node.cloneNode(true);
	cln.getElementsByTagName("button")[0].remove();
	cln.insertBefore(removeBtn, cln.firstChild);
	node.parentNode.insertBefore(cln, node.nextSibling);
	
}
function cloneComponent(element){
	var removeBtn = document.createElement("button");
	removeBtn.type = "button";
	removeBtn.onclick = function() {
		this.parentNode.remove();
    };
	removeBtn.innerHTML="-";
	var node = element.parentNode;
	var cln = node.cloneNode(true);
	cln.getElementsByTagName("button")[0].remove();
	cln.insertBefore(removeBtn, cln.firstChild);
	node.parentNode.insertBefore(cln, node.nextSibling);
}

function test(){
    try{
    	var $form = $('#xmlForm');
		var $first = $(":first-child", $form);
		
        var xml = $($.parseXML(rootElement()));
        make($first.attr('id') ,$first);
    	function make($ref, $source){
			$source.children("div").each(function(){
				var $ele = $(this);
				var $newComponent =  $("<"+$ele.attr('id')+">", xml);
				if(this.children.length > 0){
					make($newComponent.name, $ele);	
					$($newComponent,xml).append($newComponent);
				} else {
					$($ref,xml).append($newComponent);
				}
			});
    	}
        console.log((new XMLSerializer()).serializeToString(xml.context));
    }catch(e){
       console.log(e.message);
    }
};

function firstChild(){
	var $form = $('#xmlForm');
	var $first = $(":first-child", $form);
	return $first.attr('id');
}

function rootElement(){
	return '<?xml version="1.0" encoding="utf-8" ?><'+ firstChild() +'/>';
};

function buildXml(){
	//alert( $('#selectProfiles',top.document).find(":selected").val());
	var $root = $('<XMLDocument />');
	var $form = $('#xmlForm');
	var $first = $(":first-child", $form);
	
	var topId = $first.attr('id');
	$root.appendNewElement(topId);
	
	var $xml = $root.find(topId);
	make($xml, $first);
	
	function make($ref, $source){
		$source.children("div").each(function(){
			var $ele = $(this);
			var $newComponent = $.createElement($ele.attr('id'));
			if($ele.hasClass('element')){
				var input = $ele.find('input').val();
				var select = $ele.find('select').val();
				if(select !=''){
					$newComponent.text(select);
				}
				if(input !=''){
					$newComponent.text(input);
				}
			}
			if(this.children.length > 0){
				make($newComponent, $ele);				
			};
			$ref.append($newComponent);
		});
	} 
	uploadXml($root.html());
}

var currentHandle;
var currenBitstreamId;
var currentTagId;

function uploadXml(xml) {

	var uploadForm = new FormData();
	uploadForm.append("xml", xml);
	uploadForm.append("handle", currentHandle);
	uploadForm.append("bitstreamId", currenBitstreamId);

	$.ajax({
		url : '/dspace/api/cmdi/upload',
		data : uploadForm,
		dataType : 'text',
		processData : false,
		contentType : false,
		type : 'POST',
		success : function(data) {
		},
		error: function(data) {
		}
	});
	var $tag = $(currentTagId).closest("td").prev().prev().prev().prev().prev();
	var filename = $tag.text() + ".cmdi";
	$(currentTagId).closest("td").prev().prev().html(filename);
}

function createCmdi(bitstreamId, handle){
	currenBitstreamId = bitstreamId;
	currentHandle = handle;
	currentTagId = "#btn_"+ bitstreamId;
	clearDialog();
	loadProfiles();
	$('#cmdi_model_div').modal('show');
    refreshSelected();
}

function clearDialog(){
	$("#cmdiFrame").attr("src","");
	$("#selectProfiles").empty();
}

function loadProfiles(){
	RefreshEventListener()
	$.getJSON( "http://localhost:9081/rest/cmdi/profiles", function( data ) {
		 var options = "";
		  $.each( data, function( key, val ) {
			  options += "<option value=" + key  + ">" + val + "</option>";
		  });
		  $("#selectProfiles").append(options);
	});
}

function refreshSelected(){
	  var id = $('#selectProfiles').find(":selected").val();
	  if(id === "undefined" || id === 0){
	  } else {
	    var link = "http://localhost:9081/rest/cmdi/profiles/" + id + "/form";
	     $.ajax({
                type: "GET",
                url: link
            }).done(function(data){
        		$("#cmdiFrame").html(data);
            });
	  }
}

function RefreshEventListener() {
    // Remove handler from existing elements
    $("#selectProfiles").off();

    // Re-add event handler for all matching elements
    $("#selectProfiles").on("change", function() {
       	refreshSelected();
    });
    $("#processCmdi").on("click", function() {
		buildXml();
    });
}


jQuery(document).ready(function (){

$.createElement = function(name){
    return $('<'+name+' />');
};

$.fn.appendNewElement = function(name)
{
    this.each(function(i)
    {
        $(this).append('<'+name+' />');
    });
    return this;
};

	jQuery("<div class='modal fade' id='cmdi_model_div' role='dialog'>" +
			 "<div class='modal-dialog modal-lg'>" +
			 	"<div class='modal-content'>" +
		        "<div class='modal-header'>" +
		          "<button type='button' class='close' data-dismiss='modal'><span aria-hidden='true'>&#215;</span><span class='sr-only'>Close</span></button>" +
		          "<h4 class='modal-title'>Create cmdi file from profile</h4>" +
		        "</div>" +
		        "<div class='modal-body'>" +
		          "<span> Profile: <select id='selectProfiles' class='form-control'><option value='0'>None</option></select></span>"+
		          "<div id='cmdiFrame' width='100%' height='550px'></div>" +
		        "</div>" +
		        "<div class='modal-footer'>"+
                         "<button id='processCmdi' type='button' class='btn btn-default' data-dismiss='modal'>Create</button>"+
                "</div>"+
		      "</div>" +
			 "</div>" +
			"</div>").appendTo("body");
	RefreshEventListener();
});
