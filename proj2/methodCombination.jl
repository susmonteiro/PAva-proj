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
    name::Symbol                                    # name of the generic method
    parameters::Tuple                               # parameters of the generic function
    qualifier::CombineQualifier                     # qualifier of the generic method (standard, tuple)
    methods::Dict{String, SpecificMethod}           # set with all of the generic's specific methods
    effective_methods::Dict{Symbol, Any}            # set with all of the effective methods already generated
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
            Dict{String, SpecificMethod}(),
            Dict{Symbol, Any}()
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
    let signature = Symbol(map(p -> typeof(p), arguments)),
        effective_method = get(genericFunction.effective_methods, signature) do
            let before_methods = executeMethods(genericFunction.methods, BeforeQualifier(), arguments...),
                primary_method = executeMethods(genericFunction, genericFunction.methods, PrimaryQualifier(), arguments...),
                after_methods = executeMethods(genericFunction.methods, AfterQualifier(), arguments...)
                generateEffectiveMethod(genericFunction, before_methods, primary_method, after_methods, signature)
            end
        end
        effective_method(arguments...)
    end
end

# Main method responsible for combining tuple methods
function combineMethods(genericFunction::GenericFunction, qualifier::TupleQualifier, arguments...)
    println("This is a Tuple combination")
    # todo
end




function no_applicable_method(f::GenericFunction, args...)
    error("No applicable method $(f.name) for arguments $args of types $(map(arg -> typeof(arg), args))")
end

function executeMethods(methods, qualifier::BeforeQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sortMethods(applicable_methods)
end

function executeMethods(genericFunction::GenericFunction, methods, qualifier::PrimaryQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sorted_methods = sortMethods(applicable_methods)
    isempty(sorted_methods) ?
        no_applicable_method(genericFunction, arguments...) :
        first(sorted_methods)
end

function executeMethods(methods, qualifier::AfterQualifier, arguments...) 
    applicable_methods = getApplicableMethods(methods, qualifier, arguments...)
    sortMethods(applicable_methods, true)
end

function getApplicableMethods(methods, qualifier, arguments...)
    filter(m -> m.qualifier == qualifier && applicable(m.nativeFunction, arguments...), collect(values(methods)))
end

function callApplicableMethods(methods, arguments...)
    for method in methods
        method.nativeFunction(arguments...)
    end
end

function generateEffectiveMethod(gf::GenericFunction, before_methods, primary_method, after_methods, signature) 
    let parameters = map(p -> p, gf.parameters),
        effective_method = (parameters...) -> begin
            foreach(m -> m.nativeFunction(parameters...), before_methods)

            return_value = primary_method.nativeFunction(parameters...)

            foreach(m -> m.nativeFunction(parameters...), after_methods)
            return return_value
        end
        setindex!(gf.effective_methods, effective_method, signature)
        return effective_method
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
