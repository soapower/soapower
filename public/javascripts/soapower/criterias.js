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
        numberOfMonths: 1,
        maxDate: "0",
        onClose: function (selectedDate) {
            $("#to").datepicker("option", "minDate", selectedDate);
        }
    });
    $("#to").datepicker({
        dateFormat: "yy-mm-dd",
        changeMonth: true,
        numberOfMonths: 1,
        maxDate: "0",
        onClose: function (selectedDate) {
            $("#from").datepicker("option", "maxDate", selectedDate);
        }
    });
}

function storeLocalStorage() {
    initValToStore('environmentSelect', "all");
    initValToStore('soapActionSelect', "all");
    initValToStore('statusSelect', "all");
    initValToStore('from', "yesterday", true);
    initValToStore('to', "today", true);
}

function initValToStore(key, defaultValue, isDate) {
    var val = null
    if ($('#' + key) && $('#' + key).val() && $('#' + key).val() != "undefined") {
        val = $('#' + key).val();
    } else {
        val = defaultValue
    }
    if (isDate) {
        localStorage[key] = initDayToUrl(val, defaultValue)
    } else {
        localStorage[key] = val
    }
}

function getDay(sDay) {
    var mDate = new Date();
    var month = mDate.getMonth() + 1;
    var nb = -1;
    switch(sDay) {
        case "today" : nb = 0; break;
        case "yesterday" :
        default : nb = -1;
    }
    var day = mDate.getDate() + nb;
    if (month < 10) {
        month = "0" + month;
    }
    if (day < 10) {
        day = "0" + day;
    }
    return mDate.getFullYear() + "-" + month + "-" + day;
}

function initDayToUrl(val, defaultValue) {
    var dayInit = null;
    if (val == defaultValue) return val;
    if (val == "")  return defaultValue;

    if (val == getDay("today")) {
        dayInit = "today";
    } else if (val == getDay("yesterday")) {
        dayInit = "yesterday";
    } else {
        dayInit = val;
    }

    return dayInit;
}


function makeUrl(action) {
    storeLocalStorage();
    return "/"+ action +"/" + localStorage['environmentSelect']
        + "/"+ localStorage['soapActionSelect']
        + "/"+ localStorage['from']
        + "/"+ localStorage['to']
        + "/" + localStorage['statusSelect'] + "/";
}
