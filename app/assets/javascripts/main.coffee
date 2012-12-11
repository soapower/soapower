$(document).ready ->
  initCriterias = ->
    localStorage["environmentSelect"] = "all" if localStorage["environmentSelect"]?
    localStorage["soapActionSelect"] = "all" if localStorage["soapActionSelect"]?
    localStorage["statusSelect"] = "all" if localStorage["statusSelect"]?
    localStorage["from"] = "all" if localStorage["from"]?
    localStorage["to"] = "today" if localStorage["to"]?

  getUrl = (prefix) ->
    env = "all"
    env = localStorage["environmentSelect"]  if localStorage["environmentSelect"]?
    soapaction = "all"
    soapaction = localStorage["soapActionSelect"]  if localStorage["soapActionSelect"]?
    status = "all"
    status = localStorage["statusSelect"]  if localStorage["statusSelect"]?
    from = "all"
    from = localStorage["from"]  if localStorage["from"]?
    to = "today"
    if localStorage["to"]?
      to = localStorage["to"]
      sDate = getToday
      to = "today"  if sDate is to
    "/" + prefix + "/" + env + "/" + soapaction + "/" + from + "/" + to + "/" + status + "/"

  window.App = {}
  window.App.totalMemory = 400
  initCriterias

  $("#menu-search").click ->
    document.location.href = getUrl("search")

  $("#today").click ->
    today = getToday()
    $("#to").val(today)
    localStorage["to"] = "today"
    document.location.href = getUrl("search")


  $("#menu-analysis").click ->
    document.location.href = getUrl("analysis")

  $("#toggle-monitoring").click ->
    init window.App, $("#monitoring-modal")
    $("#myModal").modal keyboard: true

