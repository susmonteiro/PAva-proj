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

struct Method
    name
    parameters
    body
    native_function
end

(f::Method)(x...) = f.native_function(x...)

# todo take care of optional symbol
macro defmethod(form)
        #= if symbol != :before && symbol != :primary && symbol != :after
        # ? best exception to throw
        throw(ArgumentError("Symbol must be \"before\", \"primary\" or \"after\""))
    end =#
    let name = form.args[1].args[1],
        parameters = form.args[1].args[2:end],
        body = form.args[2]
    esc(:($(name) =
        Method(
            $(QuoteNode(name)),
            $((parameters...,)),
            $(QuoteNode(body)),
            ($(parameters...),) -> $body
        )))
    end
end

@defgeneric add(x, y)

@defmethod add(x::Int, y::Int) = x + y