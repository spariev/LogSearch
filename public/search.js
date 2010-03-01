$(document).ready(function() {
    $('.show-more-link').click(function(e){
	var url = $(this).attr('rel');
	var link = $(this);
	var trg = $(this).parent().next(".detailed-container");
	if(trg.is(":visible")) {
	    trg.addClass('hidden');
	    link.text("Show ...");
	} else {
    	$.ajax({
           url: url,
           type: 'GET',
           data: {},
           success: function(resp) {
             trg.html(resp);
	     trg.removeClass('hidden');
	     link.text(" Hide ...");
	     return false;
           },
           error: function(request) {
	     alert("errors!");
     	     return false;
           }
         });

	}    
	return false;
    });
});