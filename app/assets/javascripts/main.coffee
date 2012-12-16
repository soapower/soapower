$(document).ready ->
  window.App = {}
  window.App.totalMemory = 400

  $("#menu-search").click ->
    document.location.href = makeUrl("search")

  $("#today").click ->
    today = getToday()
    $("#to").val(today)
    localStorage["to"] = "today"
    document.location.href = makeUrl("search")

  $("#menu-analysis").click ->
    document.location.href = makeUrl("analysis")

  $("#menu-statistics").click ->
    document.location.href = makeUrl("stats")

  $("#toggle-monitoring").click ->
    init window.App, $("#monitoring-modal")
    $("#myModal").modal keyboard: true

