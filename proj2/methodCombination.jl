struct GenericFunction
    name
    parameters
    symbol
end

macro defgeneric(form, symbol=:standard)
    if symbol != :standard && symbol != :tuple
        # ? best exception to throw
        throw(ArgumentError("Symbol must be \"standard\" or \"tuple\""))
    end
    let name = form.args[1],
        parameters = form.args[2:end],
        symbol = symbol
    esc(:($(name) = 
        GenericFunction(
            $(QuoteNode(name)),
            $((parameters...,)),
            $(QuoteNode(symbol))
        )))
    end
end

@defgeneric add(x, y)