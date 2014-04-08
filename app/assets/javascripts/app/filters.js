'use strict';

    /* Filters */

spApp.filter("customAndSearch", function(){
    return function(input, searchText){
        var returnArray = [];
        if (!searchText) return input;

        var searchTextSplit = searchText.toLowerCase().split(' ');
        for(var x = 0; x < input.length; x++){
            var count = 0;
            for(var y = 0; y < searchTextSplit.length; y++){
                for (var proper in input[x]) {
                    if(input[x][proper].toString().toLowerCase().indexOf(searchTextSplit[y]) !== -1){
                        count++;
                        break;
                    }
                }
            }
            if(count == searchTextSplit.length){
                returnArray.push(input[x]);
            }
            // OR way: if (count > 0) returnArray.push(input[x]);
        }
        return returnArray;
    }
});

