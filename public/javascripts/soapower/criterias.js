function initCriterias(action) {

    $('#environmentSelect').change(function() {
        document.location.href=makeUrl(action);
    });

    $('#soapActionSelect').change(function() {
        document.location.href=makeUrl(action);
    });
    $('#statusSelect').change(function() {
        document.location.href=makeUrl(action);
    });

    $('#from').change(function() {
        document.location.href=makeUrl(action);
    });
    $('#to').change(function() {
        document.location.href=makeUrl(action);
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
}

function storeLocalStorage() {
    localStorage['environmentSelect'] = $('#environmentSelect').val();
    localStorage['soapActionSelect'] = $('#soapActionSelect').val();
    localStorage['statusSelect'] = $('#statusSelect').val();
    localStorage['from'] = $('#from').val();
    localStorage['to'] = $('#to').val();
}

function getToday() {
    var mDate = new Date();
    var month = mDate.getMonth() + 1;
    var day = mDate.getDate();
    if (month < 10) {
        month = "0" + month;
    }
    if (day < 10) {
        day = "0" + day;
    }
    return mDate.getFullYear() + "-" + month + "-" + day;
}


function makeUrl(action) {
    storeLocalStorage();

    var minDate = $('#from').val();
    var maxDate = $('#to').val();
    if (minDate == "") minDate = "all";
    if (maxDate == "") maxDate = "today";

    var today = getToday();
    if (maxDate == today) {
        maxDate = "today";
        localStorage['to'] = maxDate;
    }

    return "/"+ action +"/" + $('#environmentSelect').val()
        + "/"+ $('#soapActionSelect').val()
        +"/"+ minDate
        +"/"+ maxDate
        +"/" + $('#statusSelect').val() + "/";
}