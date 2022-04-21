# Method Combination in Julia
# Authors:
# André Nascimento      92419       https://github.com/ArcKenimuZ
# Susana Monteiro       92560       https://github.com/susmonteiro



struct SpecificMethod
    name::Symbol                # name of the specific method
    parameters::Tuple           # tuple of parameter types of the specific method
    qualifier::Symbol           # qualifier of the specific method (primary, before, after)
    nativeFunction                # anonymous function that executes the specific method
end
struct GenericFunction
    name::Symbol                    # name of the generic method
    parameters::Tuple               # parameter types of the generic method
    qualifier::Symbol               # qualifier of the generic method (standard, tuple)
    methods::Set{SpecificMethod}    # set with all of the generic's specific methods
end

(genericFunction::GenericFunction)(args...) = combineMethods(genericFunction, args...)



# Auxiliary functions to validate the generic and specific methods

function validateCombineQualifier(qualifier)
    if !(typeof(qualifier) <: Symbol && qualifier in [:standard, :tuple])
        throw(ArgumentError("GenericFunction qualifier must be \":standard\" or \":tuple\"!"))
    end
end

function validateMethodQualifier(qualifier)
    if !(typeof(qualifier) <: Symbol && qualifier in [:primary, :before, :after])
        throw(ArgumentError("SpecificMethod method qualifier must be \":primary\", \":before\" or \":after\"!"))
    end
end

function isMethodFormValid(form)::Bool
    return hasproperty(form, :args) && length(form.args) >= 1
end
    
function validateGenericFunctionForm(form)
    if !(isMethodFormValid(form) && !hasproperty(form.args[1], :args))
        throw(ArgumentError("Generic method form must be a valid generic method declaration without return type!"))
    end
end

function validateSpecificMethodForm(form)
    if !(isMethodFormValid(form) && isMethodFormValid(form.args[1]) && isMethodFormValid(form.args[2]))
        throw(ArgumentError("Specific method form must be a valid specific method declaration with a body and without return type!"))
    end
end

function validateGeneric(name::Symbol, parameters::Vector)
    try
        let generic = eval(name)
            if !(typeof(generic) <: GenericFunction && length(generic.parameters) == length(parameters))
                throw(ArgumentError(""))
            end
        end
    catch
        throw(ArgumentError("Specific method definition requires valid generic method to be defined!"))
    end
end



# Macro to define a generic method
macro defgeneric(form, qualifier=:standard)
    validateCombineQualifier(qualifier)
    validateGenericFunctionForm(form)
    let name = form.args[1],
        parameters = form.args[2:end]
        esc(:($(name) = GenericFunction(
            $(QuoteNode(name)),
            $((parameters...,)),
            $(QuoteNode(qualifier)),
            Set{SpecificMethod}()
        )))
    end
end



# Macro to define a specific method
macro defmethod(qualifier, form)
    validateMethodQualifier(qualifier)
    validateSpecificMethodForm(form)
    let name = form.args[1].args[1],
        parameters = form.args[1].args[2:end],
        body = form.args[2]
        validateGeneric(name, parameters)
        esc(:(push!($(name).methods, SpecificMethod(
            $(QuoteNode(name)),
            $((parameters...,)),
            $(QuoteNode(qualifier)),
            ($(parameters...),) -> $body
        ))))
    end
end

macro defmethod(form)
    :@defmethod primary $form
end



# todo should be done with multiple dispatch instead (standard, tuple, etc should be objects)
function combineMethods(genericFunction::GenericFunction, arguments...)
    if genericFunction.qualifier == :standard
        standardCombination(genericFunction, arguments...)
    else
        tupleCombination(genericFunction, arguments...)
    end
end



function no_applicable_method(f::GenericFunction, args...)
    # todo print types like (x,y) instead of Tuple{x,y}
    error("No applicable method for arguments $args of types $(typeof(args))")
end


function standardCombination(genericFunction::GenericFunction, arguments...)
    applicable_methods_before = getApplicableMethods(genericFunction.methods, :before, arguments...)
    callApplicableMethods(applicable_methods_before, arguments...)

    applicable_methods = getApplicableMethods(genericFunction.methods, :primary, arguments...)
    if isempty(applicable_methods)
        no_applicable_method(genericFunction, arguments...)
    else
        # todo call the most specific instead of the first one
        first(applicable_methods).nativeFunction(arguments...)
    end

    applicable_methods_after = getApplicableMethods(genericFunction.methods, :after, arguments...)
    callApplicableMethods(applicable_methods_after, arguments...)
end

function tupleCombination(genericFunction::GenericFunction, arguments...)
    # todo
end

# todo probably we should have 3 functions, each for the corresponding qualifier and then call them using multiple dispatch
function getApplicableMethods(methods, qualifier, arguments...)
    applicable_methods = []
    for method in methods
        if method.qualifier == qualifier && applicable(method.nativeFunction, arguments...)
            push!(applicable_methods, method)
        end
    end
    return applicable_methods
end

function callApplicableMethods(methods, arguments...)
    for method in methods
        method.nativeFunction(arguments...)
    end
end

function sortMethods(methods, reverse = false)
    # ! not working but close (maybe)
    sort(methods, by = x -> x.parameters, lt = (x,y) -> x <: y, rev = reverse)
end


# todo remove this
@defgeneric explain(entity)

@defmethod explain(entity::Int) = print("$entity is a Int")

@defmethod explain(entity::Rational) = print("$entity is a Rational")

@defmethod explain(entity::String) = print("$entity is a String")

@defmethod after explain(entity::Int) = print(" (in binary, is $(string(entity, base=2)))")

@defmethod before explain(entity::Real) = print("The number ")
