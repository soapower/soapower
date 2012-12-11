function storeLocalStorage() {
    localStorage['environmentSelect'] = $('#environmentSelect').val();
    localStorage['soapActionSelect'] = $('#soapActionSelect').val();
    localStorage['statusSelect'] = $('#statusSelect').val();
    localStorage['from'] = $('#from').val();
    localStorage['to'] = $('#to').val();
}

/*function retrieveLocalStorage() {
    var change = false;
    if (localStorage['environmentSelect'] != null &&
        localStorage['environmentSelect'] != $('#environmentSelect').val()) {
        $('#environmentSelect').val(localStorage['environmentSelect']);
        change = true;
    }
    if (localStorage['soapActionSelect'] != null &&
        localStorage['soapActionSelect'] != $('#soapActionSelect').val()) {
        $('#soapActionSelect').val(localStorage['soapActionSelect']);
        change = true;
    }
    if (localStorage['statusSelect'] != null &&
        localStorage['statusSelect'] != $('#statusSelect').val()) {
        $('#statusSelect').val(localStorage['statusSelect']);
        change = true;
    }
    if (localStorage['from'] != null &&
        localStorage['from'] != $('#from').val()) {
        $('#from').val(localStorage['from']);
        change = true;
    }
    if (localStorage['to'] != null &&
        localStorage['to'] != $('#to').val()) {
        $('#to').val(localStorage['to']);
        change = true;
    }
    if (change) document.location.href=makeUrl();
}
*/