var client = new XMLHttpRequest();

function upload(id,handle,bitstreamId) {
	
	var file = document.getElementById(id);
	var icon = "loading-icon-"+id;
	var idTag = "#"+id;
	var tagOk = "ok-"+id;
	$("#"+tagOk).remove();
	
	$("<i id='"+ icon+"' class='fa fa-spinner fa-spin'></i>").insertAfter( idTag );
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
			$("#"+icon).remove();
			$("<i id='"+tagOk+"' style='color:green' class='fa fa-check-circle'></i>").insertAfter( idTag );
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
	alert( $('#selectProfiles',top.document).find(":selected").val());
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

function createCmdi(handle,bitstreamId){
	$("#dialog").dialog({
		autoOpen: false,
		modal: true,
		width: 900,
		height: 600,
		beforeClose: function( event, ui ) {
			clearDialog();
		 },
		open: function( event, ui ) {
			loadProfiles();
			refreshSelected();
		},
		buttons: {
		       "Send": function() {
		    	   buildXml();
		    	   $( this ).dialog( "close" );
		        },
		        Cancel: function() {
		          $( this ).dialog( "close" );
		        }
		      }
	});
	$("#dialog").dialog("open");
}

function clearDialog(){
	$("#cmdiFrame").attr("src","");
	$("#selectProfiles").empty();
}

function loadProfiles(){
	$.getJSON( "http://localhost:9081/dspace/api/cmdi/profiles", function( data ) {
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
		  $("#cmdiFrame").attr("src", "");
	  } else {
		  $("#cmdiFrame").attr("src", "http://localhost:9081/dspace/api/cmdi/profiles/" +id);
	  }
}

$("#selectProfiles").on("change", function(e) {
	refreshSelected();
});


