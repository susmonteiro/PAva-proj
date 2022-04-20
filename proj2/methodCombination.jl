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

(f::Generic)(args...) = combineMethods(f, args...)

function no_applicable_method(f::Generic, args...)
    # todo print types like (x,y) instead of Tuple{x,y}
    error("No applicable method for arguments $args of types $(typeof(args))")
end


# todo should be done with multiple dispatch instead (standard, tuple, etc should be objects)
function combineMethods(genericMethod::Generic, arguments...)
    if genericMethod.qualifier == :standard
        standardCombination(genericMethod, arguments...)
    else
        tupleCombination(genericMethod, arguments...)
    end
end

function standardCombination(genericMethod::Generic, arguments...)
    getApplicableMethods(genericMethod.methods, :before, arguments...)
    found = getApplicableMethods(genericMethod.methods, :primary, arguments...)
    if !found
        no_applicable_method(genericMethod, arguments...)
    end
    getApplicableMethods(genericMethod.methods, :after, arguments...)
end

function tupleCombination(genericMethod::Generic, arguments...)
    # todo
end

# todo probably we should have 3 functions, each for the corresponding qualifier and then call them using multiple dispatch
function getApplicableMethods(methods, qualifier, arguments...)
    found = false
    for method in methods
        if method.qualifier == qualifier && applicable(method.native_function, arguments...)
            method.native_function(arguments...)
            found = true
        end
    end
    return found
end

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



# todo remove this
@defgeneric explain(entity)

@defmethod explain(entity::Int) = print("$entity is a Int")

@defmethod explain(entity::Rational) = print("$entity is a Rational")

@defmethod explain(entity::String) = print("$entity is a String")

@defmethod after explain(entity::Int) = print(" (in binary, is $(string(entity, base=2)))")

@defmethod before explain(entity::Real) = print("The number ")
