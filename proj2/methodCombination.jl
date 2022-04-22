# Method Combination in Julia
# Authors:
# André Nascimento      92419       https://github.com/ArcKenimuZ
# Susana Monteiro       92560       https://github.com/susmonteiro



abstract type Qualifier end

struct StandardQualifier <: Qualifier end
struct TupleQualifier <: Qualifier end

struct BeforeQualifier <: Qualifier end
struct PrimaryQualifier <: Qualifier end
struct AfterQualifier <: Qualifier end



struct SpecificMethod
    name::Symbol                # name of the specific method
    parameters::Tuple           # tuple containing the types of the parameters of the specific method
    qualifier::Qualifier        # qualifier of the specific method (primary, before, after)
    nativeFunction              # anonymous function that executes the specific method
end

struct GenericFunction
    name::Symbol                    # name of the generic method
    nParameters::Int                # number of parameters of the generic function
    qualifier::Qualifier            # qualifier of the generic method (standard, tuple)
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

function getMethodParameterTypes(generic, parameters::Vector{Any})
    if generic.nParameters != length(parameters)
        throw(ArgumentError("The existent generic function does not match the number of arguments of the specific method"))
    end
    Tuple(map(p -> p.args[2], parameters))
end

@inline function isMethodFormValid(form)::Bool
    hasproperty(form, :args) && length(form.args) >= 1
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



# Macro to define a generic method
macro defgeneric(form, qualifier=:standard)
    validateGenericFunctionForm(form)
    let name = form.args[1],
        nParameters = length(form.args[2:end])
        esc(:($(name) = GenericFunction(
            $(QuoteNode(name)),
            $(nParameters),
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
        esc(:(push!($(name).methods, SpecificMethod(
            $(QuoteNode(name)),
            getMethodParameterTypes($(name), $(parameters)),
            $(getMethodQualifier(qualifier)),
            ($(parameters...),) -> $body
        ))))
    end
end

macro defmethod(form)
    :@defmethod primary $form
end



function combineMethods(genericFunction::GenericFunction, qualifier::StandardQualifier, arguments...)
    executeMethods(genericFunction.methods, BeforeQualifier(), arguments...)
    executeMethods(genericFunction.methods, PrimaryQualifier(), arguments...)
    executeMethods(genericFunction.methods, AfterQualifier(), arguments...)
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
    sorted_methods = sortMethods(applicable_methods)
    callApplicableMethods(sorted_methods, arguments...)
end

function executeMethods(methods, qualifier::PrimaryQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sorted_methods = sortMethods(applicable_methods)    
    if isempty(sorted_methods)
        no_applicable_method(genericFunction, arguments...)
    else
        first(sorted_methods).nativeFunction(arguments...)
    end
end

function executeMethods(methods, qualifier::AfterQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sorted_methods = sortMethods(applicable_methods, true)
    callApplicableMethods(sorted_methods, arguments...)
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
# @defgeneric explain(entity)

# @defmethod explain(entity::Int) = print("$entity is a Int")

# @defmethod explain(entity::Rational) = print("$entity is a Rational")

# @defmethod explain(entity::String) = print("$entity is a String")

# @defmethod after explain(entity::Number) = print(". This one is less specific.")

# @defmethod after explain(entity::Int) = print(" (in binary, is $(string(entity, base=2)))")

# @defmethod before explain(entity::Real) = print("The number ")

# @defmethod before explain(entity::Int) = print("Most specific first! ")

# @defmethod explain(entity::Number) = print(" and a Number ")
