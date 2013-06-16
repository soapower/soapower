$(document).ready(function() {

    $( "#minDate" ).datepicker({
        dateFormat: "yy-mm-dd",
        changeMonth: true,
        numberOfMonths: 3,
        onClose: function( selectedDate ) {
            $( "#maxDate" ).datepicker( "option", "minDate", selectedDate );
        }
    });
    $( "#maxDate" ).datepicker({
        dateFormat: "yy-mm-dd",
        changeMonth: true,
        numberOfMonths: 3,
        onClose: function( selectedDate ) {
            $( "#minDate" ).datepicker( "option", "maxDate", selectedDate );
        }
    });
});

