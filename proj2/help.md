## Stuff that might help:

For the sorting operation should be just compare the types like this in the lt function:
```
julia> Tuple{Int,AbstractString} <: Tuple{Real,Any}
true

julia> Tuple{Int, Number} >: Tuple{Number, Number}
false

julia> Tuple{Int, Number} <: Tuple{Number, Number}
true
```

Just one problem...
```
julia> Tuple{Int, Number} <: Tuple{Number, Int}
false

julia> Tuple{Int, Number} >: Tuple{Number, Int}
false
```

Display a markdown table of the values in our global environment and their respective memory usage:

```> varinfo()```