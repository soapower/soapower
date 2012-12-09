$(document).ready(function() {
    createTable();

    $('#environmentSelect').change(function() {
      document.location.href=makeUrl();
    });

    $('#soapActionSelect').change(function() {
    document.location.href=makeUrl();
    });

    $('#from').change(function() {
        document.location.href=makeUrl();
    });
    $('#to').change(function() {
        document.location.href=makeUrl();
    });

    $("#from").datepicker({
        dateFormat: "yy-mm-dd",
        changeMonth: true,
        numberOfMonths: 3,
        onClose: function (selectedDate) {
            $("#to").datepicker("option", "minDate", selectedDate);
        }
    });
    $("#to").datepicker({
        dateFormat: "yy-mm-dd",
        changeMonth: true,
        numberOfMonths: 3,
        onClose: function (selectedDate) {
            $("#from").datepicker("option", "maxDate", selectedDate);
        }
    });
});

function makeUrl() {
    var minDate = $('#from').val();
    var maxDate = $('#to').val();
    if (minDate == "") minDate = "all";
    if (maxDate == "") maxDate = "all";

    return "/search/" + $('#environmentSelect').val()
        + "/"+ $('#soapActionSelect').val()
        +"/"+ minDate
        +"/"+ maxDate +"/";
}

function createTable() {
  $('#datas').dataTable( {
    "bPaginate": true,
    "bFilter": true,
    "bSort": false,
    "bInfo": true,
    "bAutoWidth": false,
    "bLengthChange": true,
    "iDisplayLength": 10,
    "oLanguage": {"sSearch": "<span class='label'>Search in soapAction</span>"},
    "bProcessing": true,
    "bServerSide": true,
    "bDeferRender": true,
    "sAjaxSource": "listDatatable",
    "fnDrawCallback": function( oSettings ) {
        $('.popSoapAction').tooltip()
    }
  } );
}
