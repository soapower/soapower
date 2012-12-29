$(document).ready(function() {
    initCriterias("stats");
    
    if($('#environmentSelect').val() !== "all") {
    	createTable();
    	$('#usage').hide();
    } else {
    	$('#tip').hide();
    }
});

function createTable() {
  $('#datas').dataTable( {
    "bPaginate": false,
    "bFilter": true,
    "bSort": false,
    "bInfo": true,
    "bAutoWidth": false,
    "bLengthChange": true,
    "oLanguage": {"sProcessing": "<span class='label label-important'>Loading...</span>"},
    "bProcessing": true,
    "bServerSide": true,
    "bDeferRender": true,
    "sAjaxSource": "listDataTable",
    "bStateSave": true,
    "fnStateSave": function (oSettings, oData) {
      localStorage.setItem( 'DataTables_'+window.location.pathname, JSON.stringify(oData) );
    },
    "fnStateLoad": function (oSettings) {
      return JSON.parse( localStorage.getItem('DataTables_'+window.location.pathname) );
    },
    "fnDrawCallback": function( oSettings ) {
    	$('#datas tr').each(function() {
    		var responseTime = parseInt($(this).children('td:nth-child(2)').text(), 10);
    		var threshold = parseInt($(this).children('td:nth-child(3)').text(), 10);
    		if(responseTime > 0 && threshold > 0) {
    			if(responseTime > threshold) {
    				$(this).addClass('error');
    			} else if(responseTime > (threshold * 8 / 10)) {
    				$(this).addClass('warning');
    			}
    		} 
    	})
    }
  } );
};
