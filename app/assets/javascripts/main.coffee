$(document).ready ->
  window.App = {}
  window.App.totalMemory = 400
  $("#toggle-monitoring").click ->
    init window.App, $("#monitoring-modal")
    $("#myModal").modal keyboard: true

