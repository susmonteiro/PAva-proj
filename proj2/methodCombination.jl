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
    parameters::Tuple            # tuple containing the types of the parameters of the specific method
    qualifier::MethodQualifier   # qualifier of the specific method (primary, before, after)
    native_function::Any         # anonymous function that executes the specific method
end

struct GenericFunction
    name::Symbol                                    # name of the generic method
    parameters::Tuple                               # parameters of the generic function
    qualifier::CombineQualifier                     # qualifier of the generic method (standard, tuple)
    methods::Dict{Symbol, SpecificMethod}           # set with all of the generic's specific methods
    effectiveMethods::Dict{Symbol, Any}             # set with all of the effective methods already generated
end

(genericFunction::GenericFunction)(arguments...) = call_effective_method(genericFunction, arguments...)


function no_applicable_method(f::GenericFunction, args...)
    error("No applicable method $(f.name) for arguments $args of types $(map(arg -> typeof(arg), args))")
end

function clean_cache(genericFunction::GenericFunction)
    empty!(genericFunction.effectiveMethods)
end

# === Auxiliary functions to validate the generic and specific methods === #

@inline function is_method_form_valid(form)
    hasproperty(form, :args) && length(form.args) >= 1
end

function validate_generic_function(form)
    if !(is_method_form_valid(form) && !hasproperty(form.args[1], :args))
        error("Generic method form must be a valid generic method declaration without return type")
    end
end

function validate_specific_method_form(form)
    if !(is_method_form_valid(form) && is_method_form_valid(form.args[1]) && is_method_form_valid(form.args[2]))
        error("Specific method form must be a valid specific method declaration with a body and without return type")
    end
end


# === Auxiliary functions to retrieve the qualifier structure from a symbol === #

function get_combine_qualifier(qualifier)
    qualifiers = Dict(:standard => StandardQualifier(), :tuple => TupleQualifier())
    get!(qualifiers, qualifier) do
        error("GenericFunction qualifier must be \":standard\" or \":tuple\"!")
    end
end

function get_method_qualifier(qualifier)
    qualifiers = Dict(:before => BeforeQualifier(), :primary => PrimaryQualifier(), :after => AfterQualifier())
    get!(qualifiers, qualifier) do 
        error("SpecificMethod method qualifier must be \":primary\", \":before\" or \":after\"")
    end        
end


# === Auxiliary function to create specific methods === #

function create_specific_method(generic::GenericFunction, qualifier::MethodQualifier, native_function)
    let parameters = fieldtypes(methods(native_function).ms[1].sig)[2:end]
        if length(generic.parameters) != length(parameters)
            error("The existent generic function does not match the number of arguments of the specific method")
        end
        SpecificMethod(parameters, qualifier, native_function)
    end
end


# === Macros for defining generics and specific method === #

# Macro to define a generic function
macro defgeneric(form, qualifier=:standard)
    validate_generic_function(form)
    let name = form.args[1],
        parameters = form.args[2:end]
        esc(:($(name) = GenericFunction(
            $(QuoteNode(name)),
            $((parameters...,)),
            $(get_combine_qualifier(qualifier)),
            Dict{String, SpecificMethod}(),
            Dict{Symbol, Any}()
        )))
    end
end


# Macro to define a specific method
macro defmethod(qualifier, form)
    validate_specific_method_form(form)
    let name = form.args[1].args[1],
        parameters = form.args[1].args[2:end],
        body = form.args[2]
        esc(:(
            let name = $(QuoteNode(name)),
                parameterSignature = Tuple(map(p -> hasproperty(p, :args) && length(p.args) >= 1 ? p.args[2] : :Any, $parameters)),
                qualifierObj = $(get_method_qualifier(qualifier))
                qualifier = $(QuoteNode(qualifier))
                clean_cache($(name))
                let signature = Symbol(name, parameterSignature, :([$qualifier])),
                    specificMethod = create_specific_method($(name), qualifierObj, ($(parameters...),) -> $body)
                    setindex!($(name).methods, specificMethod, signature)
                end
            end
        ))
    end
end

macro defmethod(form)
    :@defmethod primary $form
end


# === Functions for calling combination methods === #

# Method responsible for calling the effective method of the combination
function call_effective_method(genericFunction::GenericFunction, arguments...)
    let effective_method = retrieve_cache_methods(genericFunction, arguments...)
        effective_method(arguments...)
    end
end

# Method responsible for managing the cache of effectiveMethods
function retrieve_cache_methods(genericFunction::GenericFunction, arguments...)
    let signature = Symbol(map(p -> Symbol(typeof(p)), arguments))
        get(genericFunction.effectiveMethods, signature) do
            let effective_method = combine_methods(genericFunction, genericFunction.qualifier, arguments...)
                setindex!(genericFunction.effectiveMethods, effective_method, signature)
                return effective_method
            end
        end
    end
end


# === Functions for creating combination methods === #

# Main method responsible for getting the applicable methods and generating effective methods for a standard combination
function combine_methods(genericFunction::GenericFunction, qualifier::Qualifier, arguments...)
    let methods = find_methods(genericFunction, qualifier, arguments...)
        sort_and_generate_method(genericFunction, qualifier, methods, arguments...)
    end
end

# Method responsible for retrieving the all three types of applicable methods of a standard combination separately
function find_methods(genericFunction::GenericFunction, qualifier::StandardQualifier, arguments...)
    function add_method_to_standard_list_if_applicable!(standardMethods::Dict, method::SpecificMethod, qualifier::Qualifier, arguments...)
        if method.qualifier == qualifier && applicable(method.native_function, arguments...)
            push!(standardMethods[qualifier], method)
        end
    end

    let standardMethods = Dict(BeforeQualifier() => [], PrimaryQualifier() => [], AfterQualifier() => [])
        foreach(m -> add_method_to_standard_list_if_applicable!(standardMethods, m, m.qualifier, arguments...), collect(values(genericFunction.methods)))
        all(map(m -> isempty(m), values(standardMethods))) ?
            no_applicable_method(genericFunction, arguments...) :
            return standardMethods
    end
end

# Method responsible for retrieving all applicable methods of a tuple combination
function find_methods(genericFunction::GenericFunction, qualifier::TupleQualifier, arguments...)
    simpleMethods = filter(m -> applicable(m.native_function, arguments...), collect(values(genericFunction.methods)))
    isempty(simpleMethods) ?
        no_applicable_method(genericFunction, arguments...) :
        return simpleMethods
end

# Method responsible for generating an effective method for a standard combination
function sort_and_generate_method(genericFunction::GenericFunction, qualifier::StandardQualifier, standardMethods::Dict, arguments...) 
    let beforeMethods = sort_methods(standardMethods[BeforeQualifier()]),
        primaryMethods = sort_methods(standardMethods[PrimaryQualifier()]),
        afterMethods = sort_methods(standardMethods[AfterQualifier()], true)

        isempty(primaryMethods) ?
            generate_standard_method(beforeMethods, afterMethods) :
            generate_standard_method(beforeMethods, first(primaryMethods), afterMethods)
    end
end

# Method responsible for generating an effective method for a tuple combination
function sort_and_generate_method(genericFunction::GenericFunction, qualifier::TupleQualifier, tupleMethods::Vector{SpecificMethod}, arguments...) 
    let methods = sort_methods(tupleMethods)
        generate_tuple_method(methods)
    end
end


function generate_standard_method(beforeMethods, afterMethods)
    (parameters...) -> begin
        foreach(m -> m.native_function(parameters...), beforeMethods)
        foreach(m -> m.native_function(parameters...), afterMethods)
    end
end

function generate_standard_method(beforeMethods, primaryMethod, afterMethods)
    (parameters...) -> begin
        foreach(m -> m.native_function(parameters...), beforeMethods)
        returnValue = primaryMethod.native_function(parameters...)
        foreach(m -> m.native_function(parameters...), afterMethods)
        return returnValue
    end
end

function generate_tuple_method(methods)
    (parameters...) -> begin
        Tuple(map(m -> m.native_function(parameters...), methods))
    end
end

# Auxiliary methods to sort methods according to specificity
function sort_methods(methods::Vector, reverseOrder::Bool = false)
    function sort_predicate(A, B) 
        for (a, b) in zip(A, B)
            a == b ? continue : return (a <: b)
        end
    end

    sort(methods, by = x -> x.parameters, lt = (x,y) -> sort_predicate(x, y), rev = reverseOrder)
end