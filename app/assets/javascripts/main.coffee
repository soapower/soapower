$(document).ready ->
  initCriterias = ->
    localStorage["environmentSelect"] = "all"  if not localStorage["environmentSelect"] or localStorage["environmentSelect"] is "undefined"
    localStorage["soapActionSelect"] = "all" if not localStorage["soapActionSelect"] or localStorage["soapActionSelect"] is "undefined"
    localStorage["statusSelect"] = "all" if not localStorage["statusSelect"] or localStorage["statusSelect"] is "undefined"
    localStorage["from"] = "all" if not localStorage["from"] or localStorage["from"] is "undefined"
    localStorage["to"] = "today" if not localStorage["to"] or localStorage["to"] is "undefined"

  getUrl = (prefix) ->
    initCriterias
    env = "all"
    env = localStorage["environmentSelect"]  if localStorage["environmentSelect"] and not localStorage["environmentSelect"] is "undefined"
    soapaction = "all"
    soapaction = localStorage["soapActionSelect"]  if localStorage["soapActionSelect"] and not localStorage["soapActionSelect"] is "undefined"
    mStatus = "all"
    mStatus = localStorage["statusSelect"]  if localStorage["statusSelect"] and not localStorage["statusSelect"] is "undefined"
    from = "yesterday"
    from = localStorage["from"]  if localStorage["from"] and not localStorage["from"] is "undefined"
    to = "today"
    if localStorage["to"] and not localStorage["to"] is "undefined"
      to = localStorage["to"]
      sDate = getToday
      to = "today"  if sDate is to
    "/" + prefix + "/" + env + "/" + soapaction + "/" + from + "/" + to + "/" + mStatus + "/"

  window.App = {}
  window.App.totalMemory = 400

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

