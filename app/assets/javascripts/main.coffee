$(document).ready ->
  window.App = {}
  window.App.totalMemory = 400

  $("#today").click ->
    today = getToday()
    $("#to").val(today)
    localStorage["to"] = "today"
    document.location.href = makeUrl("search")

  $("#menu-analysis").attr("href", makeUrl("analysis"))
  $("#menu-statistics").attr("href", makeUrl("stats"))
  $("#menu-search").attr("href", makeUrl("search"))

  $("#toggle-monitoring").click ->
    init window.App, $("#monitoring-modal")
    $("#myModal").modal keyboard: true

  # Hehe.
  if window.addEventListener
    kkeys = []
    konami = "38,38,40,40,37,39,37,39,66,65"
    window.addEventListener "keydown", ((e) ->
      kkeys.push e.keyCode
      document['body'].style.backgroundImage="url('http://fierdetredeveloppeur.org/wp-content/uploads/2012/02/fierdetredev3.png')" if kkeys.toString().indexOf(konami) >= 0
    ), true