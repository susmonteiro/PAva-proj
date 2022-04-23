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



struct SpecificMethod
    name::Symbol                # name of the specific method
    parameters::Tuple           # tuple containing the types of the parameters of the specific method
    qualifier::MethodQualifier  # qualifier of the specific method (primary, before, after)
    nativeFunction::Any         # anonymous function that executes the specific method
end

struct GenericFunction
    name::Symbol                                    # name of the generic method
    parameters::Tuple                               # parameters of the generic function
    qualifier::CombineQualifier                     # qualifier of the generic method (standard, tuple)
    methods::Dict{Symbol, SpecificMethod}           # set with all of the generic's specific methods
    effectiveMethods::Dict{Symbol, Any}             # set with all of the effective methods already generated
end

(genericFunction::GenericFunction)(arguments...) = callEffectiveMethod(genericFunction, arguments...)



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

function cleanCache(genericFunction::GenericFunction)
    empty!(genericFunction.effectiveMethods)
end

function no_applicable_method(f::GenericFunction, args...)
    error("No applicable method $(f.name) for arguments $args of types $(map(arg -> typeof(arg), args))")
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
            Dict{String, SpecificMethod}(),
            Dict{Symbol, Any}()
        )))
    end
end

# Macro to define a specific method
macro defmethod(qualifier, form)
    # TODO do we need to check if the generic qualifier is tuple and the specific is before, for example? (this should not be allowed)
    validateSpecificMethodForm(form)
    let name = form.args[1].args[1],
        parameters = form.args[1].args[2:end],
        body = form.args[2]
        esc(:(
            let name = $(QuoteNode(name)),
                parameterSignature = getMethodParameterSignature($(parameters)),
                qualifierObj = $(getMethodQualifier(qualifier)),
                qualifier = $(QuoteNode(qualifier))
                cleanCache($(name))
                let signature = Symbol(name, parameterSignature, :([$qualifier])),
                    specificMethod = createSpecificMethod($(name), name, qualifierObj, ($(parameters...),) -> $body)
                    setindex!($(name).methods, specificMethod, signature)
                end
            end
        ))
    end
end

macro defmethod(form)
    :@defmethod primary $form
end



# Method responsible for calling the effective method of the combination
function callEffectiveMethod(genericFunction::GenericFunction, arguments...)
    let effectiveMethod = retrieveCachedMethods(genericFunction, arguments...)
        effectiveMethod(arguments...)
    end
end



# Method responsible for managing the cache of effectiveMethods
function retrieveCachedMethods(genericFunction::GenericFunction, arguments...)
    let signature = Symbol(map(p -> Symbol(typeof(p)), arguments))
        get(genericFunction.effectiveMethods, signature) do
            let effectiveMethod = combineMethods(genericFunction, genericFunction.qualifier, arguments...)
                setindex!(genericFunction.effectiveMethods, effectiveMethod, signature)
                return effectiveMethod
            end
        end
    end
end



# Main method responsible for getting the applicable methods and generating effective methods for a StandardCombination
function combineMethods(genericFunction::GenericFunction, qualifier::StandardQualifier, arguments...)
    let standardMethods = findMethods(genericFunction, qualifier, arguments...)
        generateEffectiveMethod(genericFunction, qualifier, standardMethods, arguments...)
    end
end

# Main method responsible for getting the applicable methods and generating effective methods for a TupleCombination
function combineMethods(genericFunction::GenericFunction, qualifier::TupleQualifier, arguments...)
    let tupleMethods = findMethods(genericFunction, qualifier, arguments...)
        generateEffectiveMethod(genericFunction, qualifier, tupleMethods, arguments...)
    end
end



# Method responsible for generating an effective method for a standard combination
function generateEffectiveMethod(genericFunction::GenericFunction, qualifier::StandardQualifier, standardMethods::Dict, arguments...) 
    let beforeMethods = sortMethods(standardMethods[BeforeQualifier()]),
        primaryMethods = sortMethods(standardMethods[PrimaryQualifier()]),
        afterMethods = sortMethods(standardMethods[AfterQualifier()], true)

        if isempty(beforeMethods) && isempty(primaryMethods) && isempty(afterMethods)
            no_applicable_method(genericFunction, arguments...)
        end

        isempty(primaryMethods) ?
            generateStandardMethod(beforeMethods, afterMethods) :
            generateStandardMethod(beforeMethods, first(primaryMethods), afterMethods)
    end
end

function generateEffectiveMethod(genericFunction::GenericFunction, qualifier::TupleQualifier, tupleMethods::Vector{SpecificMethod}, arguments...) 
    let methods = sortMethods(tupleMethods)
        isempty(methods) ?
            no_applicable_method(genericFunction, arguments...) :            
            generateTupleMethod(methods)
    end
end

function generateStandardMethod(beforeMethods, afterMethods)
    (parameters...) -> begin
        foreach(m -> m.nativeFunction(parameters...), beforeMethods)
        foreach(m -> m.nativeFunction(parameters...), afterMethods)
    end
end

function generateStandardMethod(beforeMethods, primaryMethod, afterMethods)
    (parameters...) -> begin
        foreach(m -> m.nativeFunction(parameters...), beforeMethods)
        returnValue = primaryMethod.nativeFunction(parameters...)
        foreach(m -> m.nativeFunction(parameters...), afterMethods)
        return returnValue
    end
end

function generateTupleMethod(methods)
    (parameters...) -> begin
        Tuple(map(m -> m.nativeFunction(parameters...), methods))
    end
end




# Method responsible for retrieving the all three types of applicable methods of a StandardCombination in their respective order 
function findMethods(genericFunction::GenericFunction, qualifier::StandardQualifier, arguments...)
    function addMethodToStandardListIfApplicable!(standardMethods::Dict, method::SpecificMethod, qualifier::Qualifier, arguments...)
        if method.qualifier == qualifier && applicable(method.nativeFunction, arguments...)
            push!(standardMethods[qualifier], method)
        end
    end

    let standardMethods = Dict(BeforeQualifier() => [], PrimaryQualifier() => [], AfterQualifier() => [])
        foreach(m -> addMethodToStandardListIfApplicable!(standardMethods, m, m.qualifier, arguments...), collect(values(genericFunction.methods)))
        return standardMethods
    end
end

function findMethods(genericFunction::GenericFunction, qualifier::TupleQualifier, arguments...)
    filter(m -> applicable(m.nativeFunction, arguments...), collect(values(genericFunction.methods)))
end



# Auxiliary methods used while generating the new combination method

function sortMethods(methods::Vector, reverseOrder::Bool = false)
    function sortPredicate(A, B)    # TODO: Replace by foreach
        for (a, b) in zip(A, B)
            a == b ? continue : return (a <: b)
        end
    end

    sort(methods, by = x -> x.parameters, lt = (x,y) -> sortPredicate(x, y), rev = reverseOrder)
end