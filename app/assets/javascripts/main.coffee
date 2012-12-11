$(document).ready ->
  getUrl = (prefix, suffix) ->
    env = "all"
    env = localStorage["environmentSelect"]  if localStorage["environmentSelect"]?
    soapaction = "all"
    soapaction = localStorage["soapActionSelect"]  if localStorage["soapActionSelect"]?
    from = "all"
    from = localStorage["from"]  if localStorage["from"]?
    to = "today"
    if localStorage["to"]?
      to = localStorage["to"]
      mDate = new Date()
      month = mDate.getMonth() + 1
      day = mDate.getDate()
      month = "0" + month  if month < 10
      day = "0" + day  if day < 10
      sDate = mDate.getFullYear() + "-" + month + "-" + day
      console.log "Date:" + sDate
      to = "today"  if sDate is to
      "/" + prefix + "/" + env + "/" + soapaction + "/" + from + "/" + to + "/" + suffix

  window.App = {}
  window.App.totalMemory = 400
  $("#menu-search").click ->
    document.location.href = getUrl("search", "")

  $("#menu-analysis").click ->
    document.location.href = getUrl("analysis", "200/")

  $("#toggle-monitoring").click ->
    init window.App, $("#monitoring-modal")
    $("#myModal").modal keyboard: true

