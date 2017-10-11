var current_view  = "year";
var current_year  = null;
var current_month = null;
var current_day   = null;
var current_date  = null;
var current_tab   = "views";
var loaded_data = {};
var already_loaded_dates = {};

var monthNames = ["", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

jQuery(document).ready(function (){

	$.jqplot.config.enablePlugins = true;
	
    already_loaded_dates = {"views":{}, "downloads":{}};
    loaded_data = {"views":{}, "downloads":{}};
			
	loadContents();	
	
	$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
		current_tab = e.target.getAttribute("aria-controls");
		loadContents();
	});
	
	$("#current_span_btn").click(function (){
		if(current_view == "year") {
			current_month = null;
			current_year = null;
			current_date = null;			
		} else if(current_view == "month") {
			current_view = "year";
			current_month = null;
			current_year = null;
			current_date = null;
		} else if(current_view == "day") {
			current_view = "month";
			current_date = current_year;
		}
		loadContents();
	});            
	

});

loadContents = function () {
	
	jQuery('#visits_over_time_chart').html('<div class="piwik-loading" style="width: 100%; height: 100%; z-index=1; display: none;"><i class="fa fa-pulse fa-3x" >&#xf110;</i></div>');
	jQuery('#downloads_over_time_chart').html('<div class="piwik-loading" style="width: 100%; height: 100%; z-index=1; display: none;"><i class="fa fa-pulse fa-3x" >&#xf110;</i></div>');	
	jQuery(".piwik-loading").css("display", "block");	

	var targetDiv = jQuery("#piwik-charts");		
    var reportURL = targetDiv.attr("data-url");
    reportURL += "?period=" + current_view;
    
    if(current_date!=null) {
    	reportURL += "&date=" + current_date;
    }
		
	var visitsPlot;
	var downloadPlot;			
	
    if(!(current_view in already_loaded_dates["views"] && current_date in already_loaded_dates["views"][current_view])) {
    	$.getJSON(reportURL, function(data) {
            loaded_data["views"] = $.extend(true, loaded_data["views"], data["response"]["views"]);
            loaded_data["downloads"] = $.extend(true, loaded_data["downloads"], data["response"]["downloads"]);
            if(!(current_view in already_loaded_dates["views"])) already_loaded_dates["views"][current_view] = {};
            if(!(current_date in already_loaded_dates["views"][current_view])) already_loaded_dates["views"][current_view][current_date] = true;

        	if(loaded_data == null || loaded_data["views"] == null || Object.keys(loaded_data["views"]).length <= 1) {
        		$("#piwik-charts-msg").html("<div class='alert alert-info'><strong>No statistics available for this item yet.</strong></div>");
        		$("#piwik-charts").addClass("hide");		
        		$("#piwik-charts-msg").removeClass("hide");
        		return;
        	} else {
        		plot();
        	}

        })
	} else {
		plot();
	}    	
}	

plotViews = function (div, data, color, tf, ti, highlightString) {

	var ticks = [];
	
	if(current_view == "year") {
		try{
			ticks = Object.keys(data)
	                .filter(function(e) { return e !== 'total' && !e.startsWith('nb') })
	                .sort(function(a,b){return parseInt(a)-parseInt(b)});
		}catch(e){
			var cy = new Date().getFullYear();
			for(var i=cy-5;i<=cy;i++) {
				ticks.push("" + i);
			}
		}
		if(ticks.length==1) {
            current_view = "month";
            current_year = ticks[0];
            current_date = current_year;
            loadContents();
            return;
		}		
	}
	else
	if(current_view == "month") {
		ticks = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"];
	}
	else
	if(current_view == "day") {
		for(i=1;i<=moment(current_date, "YYYY-M").daysInMonth();i++) {
			ticks.push("" + i);
		}
	}
	
	var x = [];
	var y = [];
	var locations = {};
	for(index in ticks) {
		var tick = ticks[index];
		if(current_view == "month") {
			tick = current_year + "-" + tick;
		} else
		if(current_view == "day") {
			tick = current_year + "-" + current_month + "-" + tick;
		}
		x.push(tick);
		if(data != undefined && data[ticks[index]]) {
			var v = data[ticks[index]]["nb_hits"];		
			y.push([tick, v]);
		} else {
			y.push([tick, 0]);
		}
	}

	$("#" + div).html("");

	var xa = { 	renderer : $.jqplot.DateAxisRenderer,
				tickOptions : {formatString:tf},
				tickInterval : ti };

	if(current_view == "year") {
		xa["min"] = x[0];
		xa["max"] = x[x.length-1];
	} else if(current_view == "month") {
		xa["min"] = current_year + "-01";
		xa["max"] = current_year + "-12";
	} else if(current_view == "day") {
		xa["min"] = current_year + "-" + current_month + "-01";
		xa["max"] = current_year + "-" + current_month + "-" + new Date(current_year, current_month, 0).getDate();
	}

	var p = $.jqplot(div, [ y, y ], {

		axes : {
			xaxis : xa,
			yaxis : {
				min : 0,
				tickOptions : {
					formatter : tickFormatter,
				}
			}
		},

		highlighter: {
			show: true,
			sizeAdjust: 7.5,
  			tooltipAxes: "both",
		},

		cursor: {
	        show: false,
		},

		seriesDefaults: {
			lineWidth:4,
  			shadow:false,
  			markerOptions: {
  				size: 7
  			},			
			highlighter: {formatString: highlightString},
			pointLabels: { show:false },
			breakOnNull: true
	  	},
	  	
	  	grid: {background: '#F0F0F0', borderWidth: 0, shadow: false},
	  	
	    seriesColors: color,
	    
	    series: [{fill: [true, false]}],	    	  	

	});

	$('#' + div).unbind('jqplotDataClick');

	$('#' + div).bind('jqplotDataClick',
        function (ev, seriesIndex, pointIndex, d) {
            if(current_view == "year") {
            	current_view = "month";
            	current_year = ticks[pointIndex];
            	current_date = current_year;
            } else if(current_view == "month") {
            	current_view = "day";
            	current_month = ticks[pointIndex];
            	current_date = current_year + "-" + current_month;
            }
            
            loadContents();
        }
	);
	
	return p;
}


tickFormatter = function (format, val) {
    if (val >= 1000000) {
        val = val / 1000000;
    return val.toFixed(1)+"M";
    }
    if (val >= 1000) {
        val = val / 1000;
            if (val < 10) {
                return val.toFixed(1)+"K";
            }
        return val.toFixed(1)+"K";
    }
    return val.toFixed(0);
}

plot = function () {
	
	var visitsPlot;
	var downloadPlot;
	
    var views = loaded_data["views"]["total"];
    var downloads = loaded_data["downloads"]["total"];
    var tf  = "%Y";
    var ti  = "1 year";
    
    if(current_view=="month") {
    	try{
    		views = loaded_data["views"]["total"][current_year];
    	}catch(e){}
    	try{
    		downloads = loaded_data["downloads"]["total"][current_year];
    	}catch(e){}
        tf  = "%b";
        ti  = "1 month";            	
    }
    
    if(current_view=="day") {
    	try{
    		views = loaded_data["views"]["total"][current_year][current_month];
    	}catch(e){}
    	try{
    		downloads = loaded_data["downloads"]["total"][current_year][current_month];
		}catch(e){}
        tf  = "%d";
        ti  = "1 day";            	
    }
    
    if(current_tab=="views") {
    	visitsPlot = plotViews("visits_over_time_chart", views, ["#bee89c", "#60a22a"], tf, ti, "<div style='font-size: 110%; padding: 5px; color: #FFFFFF;'>%s<BR/><strong style='font-size: 14px;'>%s</strong> Views</div>");
    } else {
    	downloadPlot = plotViews("downloads_over_time_chart", downloads, ["#94c7ea", "#1f78b4"], tf, ti, "<div style='font-size: 110%; padding: 5px; color: #FFFFFF;'>%s<BR/><strong style='font-size: 14px;'>%s</strong> Downloads</div>");
    	
		if(current_view == 'year') {
	    	var bitwiseDownloads = "<div class='container' style='margin-top: 20px;'>";
			bitwiseDownloads += "<table class='table table-striped'><thead><tr><th colspan='2'>Filewise Statistics</th></tr></thead><tbody>";			
			var years = Object.keys(loaded_data["downloads"]["total"]).sort().filter(function(e) { return !e.startsWith("nb") });
			for(var year in years) {
				
				bitwiseDownloads += "<tr><td colspan='2'>" + years[year] + "</td></tr>";
				
				var temp = loaded_data["downloads"][years[year]];
				temp = Object.keys(temp).filter(function(e){return e.length>2;}); 
				
				var map = {};			
				for(var key in temp) {
					map[temp[key]] = loaded_data["downloads"][years[year]][temp[key]]["nb_hits"]; 
				}
				map = sortMapByValue(map);
				for(var index in map) {
					bitwiseDownloads += "<tr><td class='col-md-2 text-right'><strong>" + map[index][1] + "</strong></td><td>" + getBitstreamFromURL(map[index][0]) + "</td></tr>";
				}
			}
			bitwiseDownloads += "</tbody></table>";			
		} else 
		if(current_view == 'month') {
	    	var bitwiseDownloads = "<div class='container' style='margin-top: 20px;'>";
			bitwiseDownloads += "<table class='table table-striped'><thead><tr><th colspan='2'>Filewise Statistics</th></tr></thead><tbody>";			
			
			var temp = loaded_data["downloads"][current_year];
			temp = Object.keys(temp).filter(function(e){return e.length>2;}); 
			
			var map = {};			
			for(var key in temp) {
				map[temp[key]] = loaded_data["downloads"][current_year][temp[key]]["nb_hits"]; 
			}
			map = sortMapByValue(map);
			for(var index in map) {
				bitwiseDownloads += "<tr><td class='col-md-2 text-right'><strong>" + map[index][1] + "</strong></td><td>" + getBitstreamFromURL(map[index][0]) + "</td></tr>";
			}

			
			bitwiseDownloads += "</tbody></table>";						
		} else
		if(current_view == 'day') {
	    	var bitwiseDownloads = "<div class='container' style='margin-top: 20px;'>";
			bitwiseDownloads += "<table class='table table-striped'><thead><tr><th colspan='2'>Filewise Statistics</th></tr></thead><tbody>";			
			
			var temp = loaded_data["downloads"][current_year][current_month];
			temp = Object.keys(temp).filter(function(e){return e.length>2;}); 
			
			var map = {};			
			for(var key in temp) {
				map[temp[key]] = loaded_data["downloads"][current_year][current_month][temp[key]]["nb_hits"]; 
			}
			map = sortMapByValue(map);
			for(var index in map) {
				bitwiseDownloads += "<tr><td class='col-md-2 text-right'><strong>" + map[index][1] + "</strong></td><td>" + getBitstreamFromURL(map[index][0]) + "</td></tr>";
			}

			
			bitwiseDownloads += "</tbody></table>";						
			
		}
        
    	var bitwiseDownloadsDiv = jQuery('#bitwiseDownloads');
        if(bitwiseDownloadsDiv.html()==null) {
      	  jQuery('#downloads').append('<div id="bitwiseDownloads"></div>');
        }                                  
        jQuery('#bitwiseDownloads').html(bitwiseDownloads);    	
    }
    
    var t = 0;
    var d = 0;
    
    if(current_view=="year") {
    	try {
    		t = loaded_data["views"]["total"]["nb_hits"];
    	}catch(e) {}
    	try {
    		d = loaded_data["downloads"]["total"]["nb_hits"];
    	}catch(e) {}
    	jQuery('#views_tab_count').html("<strong>" + t + "</strong>");
    	jQuery('#downloads_tab_count').html("<strong>" + d + "</strong>");
    } else 
    if(current_view=="month") {
    	try {
    		t = loaded_data["views"]["total"][current_year]["nb_hits"];
    	}catch(e) {}
    	try {
    		d = loaded_data["downloads"]["total"][current_year]["nb_hits"];
		}catch(e) {}
    	jQuery('#views_tab_count').html("<strong>" + t + "</strong>");
    	jQuery('#downloads_tab_count').html("<strong>" + d + "</strong>");            	
    } else
    if(current_view=="day") {
    	try {
    		t = loaded_data["views"]["total"][current_year][current_month]["nb_hits"];
    	}catch(e) {}
    	try {
    		d = loaded_data["downloads"]["total"][current_year][current_month]["nb_hits"];
    	}catch(e) {}
    	jQuery('#views_tab_count').html("<strong>" + t + "</strong>");
    	jQuery('#downloads_tab_count').html("<strong>" + d + "</strong>");            	
    }
    
    if(current_view == "year") {
        var years = Object.keys(loaded_data[current_tab=="views"?"views":"downloads"]["total"]).sort().filter(function(e) { return !e.startsWith("nb") });
        $(".current_span").html("Statistics for years " + years[0] + " to " + years[years.length-1]);
        $("#current_span_btn").hide();
    } else if(current_view == "month") {
        $(".current_span").html("Statistics for the year " + current_year);
        $("#current_span_btn").show();
    } else if(current_view == "day") {
        $(".current_span").html("Statistics for " + monthNames[parseInt(current_month)] + ", " + current_year);
        $("#current_span_btn").show();
    }
    
    jQuery(window).resize(function(){
		if(visitsPlot!=null) visitsPlot.replot();
		if(downloadPlot!=null) downloadPlot.replot();
    });    
    
    jQuery(".piwik-loading").css("display", "none");
    	
}


sortMapByValue = function (map){
    var tupleArray = [];
    for (var key in map) tupleArray.push([key, map[key]]);
    tupleArray.sort(function (a, b) { return b[1] - a[1] });
    return tupleArray;
}

getBitstreamFromURL = function (url) {
	var l = document.createElement("a");
	l.href = url;
	return decodeURI(l.pathname.substr(l.pathname.lastIndexOf('/') + 1));
}
