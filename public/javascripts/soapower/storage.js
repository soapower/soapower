function storeLocalStorage() {
    localStorage['environmentSelect'] = $('#environmentSelect').val();
    localStorage['soapActionSelect'] = $('#soapActionSelect').val();
    localStorage['statusSelect'] = $('#statusSelect').val();
    localStorage['from'] = $('#from').val();
    localStorage['to'] = $('#to').val();
}