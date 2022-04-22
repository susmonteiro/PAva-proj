# Method Combination in Julia
# Authors:
# André Nascimento      92419       https://github.com/ArcKenimuZ
# Susana Monteiro       92560       https://github.com/susmonteiro

struct StandardQualifier end
struct TupleQualifier end

struct BeforeQualifier end
struct PrimaryQualifier end
struct AfterQualifier end

struct SpecificMethod
    name::Symbol                # name of the specific method
    parameters::Tuple           # tuple of parameter types of the specific method
    qualifier                   # qualifier of the specific method (primary, before, after)
    body
    nativeFunction              # anonymous function that executes the specific method
end
struct GenericFunction
    name::Symbol                    # name of the generic method
    parameters::Tuple               # parameter types of the generic method
    qualifier                       # qualifier of the generic method (standard, tuple)
    methods::Set{SpecificMethod}    # set with all of the generic's specific methods
end

(genericFunction::GenericFunction)(args...) = combineMethods(genericFunction, genericFunction.qualifier, args...)



# Auxiliary functions to validate the generic and specific methods

function getCombineQualifier(qualifier)
    qualifiers = Dict(:standard => StandardQualifier(), :tuple => TupleQualifier())
    get!(qualifiers, qualifier) do
        throw(ArgumentError("GenericFunction qualifier must be \":standard\" or \":tuple\"!"))
    end
end

function getMethodQualifier(qualifier)
    qualifiers = Dict(:before => BeforeQualifier(), :primary => PrimaryQualifier(), :after => AfterQualifier())
    get!(qualifiers, qualifier) do 
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
    validateGenericFunctionForm(form)
    let name = form.args[1],
        parameters = form.args[2:end]
        esc(:($(name) = GenericFunction(
            $(QuoteNode(name)),
            $((parameters...,)),
            $(getCombineQualifier(qualifier)),
            Set{SpecificMethod}()
        )))
    end
end

# Macro to define a specific method
macro defmethod(qualifier, form)
    validateSpecificMethodForm(form)
    let name = form.args[1].args[1],
        parameters = form.args[1].args[2:end],
        body = form.args[2]
        validateGeneric(name, parameters)
        esc(:(push!($(name).methods, SpecificMethod(
            $(QuoteNode(name)),
            $(parameters...,),
            $(getMethodQualifier(qualifier)),
            $(QuoteNode(body)),
            ($(parameters...),) -> $body
        ))))
    end
end

macro defmethod(form)
    :@defmethod primary $form
end



function combineMethods(genericFunction::GenericFunction, qualifier::StandardQualifier, arguments...)
    methods = []
    append!(methods, executeMethods(genericFunction.methods, BeforeQualifier(), arguments...))
    append!(methods, executeMethods(genericFunction.methods, PrimaryQualifier(), arguments...))
    append!(methods, executeMethods(genericFunction.methods, AfterQualifier(), arguments...))
    #generateEffectiveMethod(genericFunction, methods)
    println("\nDone :)")
end

function combineMethods(genericFunction::GenericFunction, qualifier::TupleQualifier, arguments...)
    println("This is a Tuple combination")
    # todo
end




function no_applicable_method(f::GenericFunction, args...)
    # todo print types like (x,y) instead of Tuple{x,y}
    error("No applicable method $(f.name) for arguments $args of types $(typeof(args))")
end

# todo change name
function executeMethods(methods, qualifier::BeforeQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sortMethods(applicable_methods)
end

function executeMethods(methods, qualifier::PrimaryQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sorted_methods = sortMethods(applicable_methods)    
    if isempty(sorted_methods)
        no_applicable_method(genericFunction, arguments...)
    else
        first(sorted_methods).nativeFunction(arguments...)
    end
    return [first(sorted_methods)]
end

function executeMethods(methods, qualifier::AfterQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sortMethods(applicable_methods, true)
end

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

# ! not working as it should yet
function generateEffectiveMethod(gf::GenericFunction, methods)
    # effective_method = :(($(gf.parameters...),) ->
    #     $(first(methods).nativeFunction)($(gf.parameters...)))
    # println("\n\nEffective Method: \n$effective_method")
    # effective_method(2)
    parameters = []
    for p in gf.parameters
        push!(parameters, p)
    end
    println(parameters)

    # this is the code we want to generate
    effective_method = (parameters) -> 
        :($(for m in methods
            :(m.nativeFunction(parameters))
        end))

    effective_method(2)
end

function sortMethods(methods, reverse = false)
    sort(methods, by = x -> x.parameters, lt = (x,y) -> sortFunction(x, y), rev = reverse)
end

function sortFunction(A, B)
    for (a, b) in zip(A, B)
        typeA = eval(a.args[2])
        typeB = eval(b.args[2])
        if (typeA == typeB) 
            println("Types are the same")
            continue
        else
            return (typeA <: typeB)
        end
    end
end

# todo remove this
@defgeneric explain(entity)

@defmethod explain(entity::Int) = print("$entity is a Int")

@defmethod explain(entity::Rational) = print("$entity is a Rational")

@defmethod explain(entity::String) = print("$entity is a String")

@defmethod after explain(entity::Number) = print(". This one is less specific.")

@defmethod after explain(entity::Int) = print(" (in binary, is $(string(entity, base=2)))")

@defmethod before explain(entity::Real) = print("The number ")

@defmethod before explain(entity::Int) = print("Most specific first! ")

@defmethod explain(entity::Number) = print(" and a Number ")

# todo take care of args with no explicit type
# @defmethod explain(entity) = print("No type name")
