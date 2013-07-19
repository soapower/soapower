# Hehe.
if window.addEventListener
  kkeys = []
  konami = "38,38,40,40,37,39,37,39,66,65"
  window.addEventListener "keydown", ((e) ->
    kkeys.push e.keyCode
    document['body'].style.backgroundImage="url('/images/fierdetredev3.png')" if kkeys.toString().indexOf(konami) >= 0
  ), true