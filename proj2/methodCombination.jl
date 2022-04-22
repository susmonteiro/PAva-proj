# Method Combination in Julia
# Authors:
# André Nascimento      92419       https://github.com/ArcKenimuZ
# Susana Monteiro       92560       https://github.com/susmonteiro



abstract type Qualifier end
abstract type CombineQualifier <: Qualifier end
abstract type MethodQualifier <: Qualifier end

struct StandardQualifier <: CombineQualifier end
struct TupleQualifier <: CombineQualifier end

struct BeforeQualifier <: MethodQualifier end
struct PrimaryQualifier <: MethodQualifier end
struct AfterQualifier <: MethodQualifier end

@inline function toString(qualifier::StandardQualifier) "StandardQualifier" end
@inline function toString(qualifier::TupleQualifier) "TupleQualifier" end
@inline function toString(qualifier::BeforeQualifier) "BeforeQualifier" end
@inline function toString(qualifier::PrimaryQualifier) "PrimaryQualifier" end
@inline function toString(qualifier::AfterQualifier) "AfterQualifier" end



struct SpecificMethod
    name::Symbol                # name of the specific method
    parameters::Tuple           # tuple containing the types of the parameters of the specific method
    qualifier::MethodQualifier  # qualifier of the specific method (primary, before, after)
    nativeFunction              # anonymous function that executes the specific method
end

struct GenericFunction
    name::Symbol                            # name of the generic method
    parameters::Tuple                       # parameters of the generic function
    qualifier::CombineQualifier             # qualifier of the generic method (standard, tuple)
    methods::Dict{String, SpecificMethod}   # set with all of the generic's specific methods
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

@inline function getMethodParameterSignature(parameters::Vector{Any})
    Tuple(map(p -> hasproperty(p, :args) && length(p.args) >= 1 ? p.args[2] : :Any, parameters))
end

@inline function isMethodFormValid(form)
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

function createSpecificMethod(generic::GenericFunction, name::Symbol, qualifier::MethodQualifier, nativeFunction)
    let parameters = fieldtypes(methods(nativeFunction).ms[1].sig)[2:end]
        if length(generic.parameters) != length(parameters)
            throw(ArgumentError("The existent generic function does not match the number of arguments of the specific method"))
        end
        SpecificMethod(name, parameters, qualifier, nativeFunction)
    end
end



# Macro to define a generic function
macro defgeneric(form, qualifier=:standard)
    validateGenericFunctionForm(form)
    let name = form.args[1],
        parameters = form.args[2:end]
        esc(:($(name) = GenericFunction(
            $(QuoteNode(name)),
            $((parameters...,)),
            $(getCombineQualifier(qualifier)),
            Dict{String, SpecificMethod}()
        )))
    end
end

# Macro to define a specific method
macro defmethod(qualifier, form)
    validateSpecificMethodForm(form)
    let name = form.args[1].args[1],
        parameters = form.args[1].args[2:end],
        body = form.args[2]
        esc(:(
            let name = $(QuoteNode(name)),
                parameterSignature = getMethodParameterSignature($(parameters)),
                qualifier = $(getMethodQualifier(qualifier))
                let signature = String(name) * "$parameterSignature[$(toString(qualifier))]"
                    specificMethod = createSpecificMethod($(name), name, qualifier, ($(parameters...),) -> $body)
                    setindex!($(name).methods, specificMethod, signature)
                end
            end
        ))
    end
end

macro defmethod(form)
    :@defmethod primary $form
end



# Main method responsible for combining standard methods
function combineMethods(genericFunction::GenericFunction, qualifier::StandardQualifier, arguments...)
    methods = []
    append!(methods, executeMethods(genericFunction.methods, BeforeQualifier(), arguments...))
    append!(methods, executeMethods(genericFunction.methods, PrimaryQualifier(), arguments...))
    append!(methods, executeMethods(genericFunction.methods, AfterQualifier(), arguments...))
    effective_method = generateEffectiveMethod(genericFunction, methods)
    effective_method(arguments...)
end

# Main method responsible for combining tuple methods
function combineMethods(genericFunction::GenericFunction, qualifier::TupleQualifier, arguments...)
    println("This is a Tuple combination")
    # todo
end




function no_applicable_method(f::GenericFunction, args...)
    # todo print types like (x,y) instead of Tuple{x,y}
    error("No applicable method $(f.name) for arguments $args of types $(typeof(args))")
end

function executeMethods(methods, qualifier::BeforeQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sortMethods(applicable_methods)
end

function executeMethods(methods, qualifier::PrimaryQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sorted_methods = sortMethods(applicable_methods)    
    if isempty(sorted_methods)
        no_applicable_method(genericFunction, arguments...)
    end
    return [first(sorted_methods)]
end

function executeMethods(methods, qualifier::AfterQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sortMethods(applicable_methods, true)
end

function getApplicableMethods(methods, qualifier, arguments...)
    applicable_methods = []
    for method in values(methods)
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
    parameters = map(p -> p, gf.parameters)

    effective_method = (parameters) -> begin
        applicable_methods = methods
        for m in applicable_methods
            m.nativeFunction(parameters)
        end
    end
end

function sortMethods(methods, reverse = false)
    sort(methods, by = x -> x.parameters, lt = (x,y) -> sortFunction(x, y), rev = reverse)
end

function sortFunction(A, B)
    for (a, b) in zip(A, B)
        if (a == b) 
            continue
        else
            return (a <: b)
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
