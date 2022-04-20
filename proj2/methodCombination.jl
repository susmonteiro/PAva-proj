struct SpecificMethod
    name
    parameters
    qualifier
    body
    native_function
end

struct Generic
    name
    parameters
    qualifier
    methods
end     

(f::Generic)(x...) = f.name(x...)

macro defgeneric(form, qualifier=:standard)
    if qualifier != :standard && qualifier != :tuple
        # ? best exception to throw
        throw(ArgumentError("qualifier must be \"standard\" or \"tuple\""))
    end
    let name = form.args[1],
        parameters = form.args[2:end],
        qualifier = qualifier
        esc(:($(name) = 
            Generic(
                $(QuoteNode(name)),
                $((parameters...,)),
                $(QuoteNode(qualifier)),
                SpecificMethod[]
        )))
    end
end

macro defmethod(form)
    :@defmethod primary $form
end

macro defmethod(qualifier, form)
    if qualifier != :before && qualifier != :primary && qualifier != :after
        # ? best exception to throw
        throw(ArgumentError("qualifier must be \"before\", \"primary\" or \"after\""))
    end
    let name = form.args[1].args[1],
        parameters = form.args[1].args[2:end],
        body = form.args[2],
        qualifier=qualifier
    esc(:(push!($(name).methods, 
        SpecificMethod(
            $(QuoteNode(name)),
            $((parameters...,)),
            $(QuoteNode(qualifier)),
            $(QuoteNode(body)),
            ($(parameters...),) -> $body
        ))))
    end
end
