$(document).ready(function() {
    initCriterias("stats");
    createTable();
});

function createTable() {
  $('#datas').dataTable( {
    "bPaginate": false,
    "bFilter": true,
    "bSort": true,
    "bInfo": true,
    "bAutoWidth": false,
    "bLengthChange": true,
    "oLanguage": {"sProcessing": "<span class='label label-important'>Loading...</span>"},
    "bProcessing": true,
    "bServerSide": false,
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
