function initCriterias(action) {
	
	$('#groupSelect').change(function() {
        document.location.href=makeUrl(action);
    });


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

    if ($('#isStatsOnly').length > 0) {
        $('#isStatsOnly').change(function() {
            document.location.href=makeUrl(action);
        });
    }

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
	initValToStore('groupSelect', "all");
    initValToStore('environmentSelect', "all");
    initValToStore('soapActionSelect', "all");
    initValToStore('statusSelect', "all");
    initValToStore('from', "yesterday", true);
    initValToStore('to', "today", true);
    initValToStore('isStatsOnly', "true")
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
    var isStatsOnly = ""
    if (action == "analysis") {
        isStatsOnly =  "true/"
        if ($('#isStatsOnly').length > 0 ) { // is we are on analysis page
            if ( $('input:checkbox[name=isStatsOnly]:checked') && $('input:checkbox[name=isStatsOnly]:checked').val() == "on") {
                isStatsOnly =  "true/"
            } else {
                isStatsOnly =  "false/"
            }
        }
    }
    return "/"+ action +"/" + localStorage['groupSelect']
   		  +"/" + localStorage['environmentSelect']
        + "/"+ localStorage['soapActionSelect']
        + "/"+ localStorage['from']
        + "/"+ localStorage['to']
        + "/" + localStorage['statusSelect'] + "/" + isStatsOnly;
}
