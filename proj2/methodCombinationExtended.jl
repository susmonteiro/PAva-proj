# Method Combination in Julia (with extensions)
# Authors:
# André Nascimento      92419       https://github.com/ArcKenimuZ
# Susana Monteiro       92560       https://github.com/susmonteiro

abstract type Qualifier end
abstract type CombineQualifier <: Qualifier end
abstract type MethodQualifier <: Qualifier end

abstract type SimpleQualifier <: CombineQualifier end
struct StandardQualifier <: CombineQualifier end

struct TupleQualifier <: SimpleQualifier end
struct AndQualifier <: SimpleQualifier end
struct OrQualifier <: SimpleQualifier end
struct SumQualifier <: SimpleQualifier end
struct ProdQualifier <: SimpleQualifier end
struct MaxQualifier <: SimpleQualifier end
struct MinQualifier <: SimpleQualifier end

function simple_operation(qualifier::TupleQualifier) Tuple end
function simple_operation(qualifier::AndQualifier) all end
function simple_operation(qualifier::OrQualifier) any end
function simple_operation(qualifier::SumQualifier) sum end
function simple_operation(qualifier::ProdQualifier) prod end
function simple_operation(qualifier::MaxQualifier) maximum end
function simple_operation(qualifier::MinQualifier) minimum end

struct BeforeQualifier <: MethodQualifier end
struct PrimaryQualifier <: MethodQualifier end
struct AfterQualifier <: MethodQualifier end

struct SpecificMethod
    parameters::Tuple            # tuple containing the types of the parameters of the specific method
    qualifier::MethodQualifier   # qualifier of the specific method (primary, before, after)
    native_function::Any         # anonymous function that executes the specific method
end

struct GenericFunction
    parameters::Tuple                               # parameters of the generic function
    parametersOrder::Tuple                          # arguments precedence order
    qualifier::CombineQualifier                     # qualifier of the generic method (standard, tuple)
    methods::Dict{Symbol, SpecificMethod}           # dictionary with all of the generic's specific methods
    effectiveMethods::Dict{Symbol, Any}             # dictionary with all of the effective methods already generated
end

struct GenericContainer
    genericFunctions::Dict{Int, GenericFunction}    # dictionary with all the generic functions for each number of arguments
end

(genericContainer::GenericContainer)(arguments...) = call_effective_method(genericContainer, arguments...)



# Auxiliary functions to create and validate the generic and specific methods

function get_combine_qualifier(qualifier)
    qualifiers = Dict(
        :standard => StandardQualifier(), 
        :tuple => TupleQualifier(),
        :and => AndQualifier(),
        :or => OrQualifier(),
        :sum => SumQualifier(),
        :prod => ProdQualifier(),
        :max => MaxQualifier(),
        :min => MinQualifier())
    get!(qualifiers, qualifier) do
        error("$qualifier is not valid. Possible values are: \"standard\" (default), \"tuple\", \"and\", \"or\", \"sum\", \"prod\", \"max\" and \"min\"")
    end
end

function get_method_qualifier(qualifier)
    qualifiers = Dict(:before => BeforeQualifier(), :primary => PrimaryQualifier(), :after => AfterQualifier())
    get!(qualifiers, qualifier) do 
        error("SpecificMethod method qualifier must be \":primary\", \":before\" or \":after\"")
    end        
end


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


function get_parameter_signature(parameters)
    Tuple(map(p -> hasproperty(p, :args) && length(p.args) >= 1 ? p.args[2] : :Any, (parameters)))
end

function get_argument_order(option, parameters, precedence)
    function findParameter(parameter)
        results = findall(p -> p == parameter, parameters)
        length(results) != 1 ?
            error("$parameter is not a valid parameter") :
            first(results)
    end

    if option != :(:precedence)
        error("The option $option is not recognized")
    elseif length(parameters) != length(precedence)
        error("The length of parameters does not match the length of the precedence list")
    end

    Tuple(map(parameter -> findParameter(parameter), precedence))
end


function create_generic_function(parameters::Tuple, argumentsOrder::Tuple, qualifier::CombineQualifier)
    GenericFunction(parameters, argumentsOrder, qualifier, Dict{String, SpecificMethod}(), Dict{Symbol, Any}())
end

function create_specific_method(generic::GenericFunction, qualifier::MethodQualifier, native_function)
    let parameters = fieldtypes(methods(native_function).ms[1].sig)[2:end]
        if length(generic.parameters) != length(parameters)
            error("The existent generic function does not match the number of arguments of the specific method")
        end
        SpecificMethod(parameters, qualifier, native_function)
    end
end


function clean_cache(genericFunction::GenericFunction)
    empty!(genericFunction.effectiveMethods)
end

function no_applicable_method(f::GenericFunction, args...)
    error("No applicable method $(f.name) for arguments $args of types $(map(arg -> typeof(arg), args))")
end


# Macro to define a generic function
macro defgeneric(form::Expr, precedenceOrder::Vector{Any}, qualifier::Symbol)
    let name = form.args[1],
        parameters = form.args[2:end],
        option = precedenceOrder[1],
        argumentsPrecedence = precedenceOrder[2:end],
        argumentsOrder = get_argument_order(option, parameters, argumentsPrecedence),
        qualifier = get_combine_qualifier(qualifier)
        esc(:(
            if !(@isdefined $name)
                GenericContainer(Dict{Int, GenericFunction}(
                    length($((parameters...,))) => create_generic_function($((parameters...,)), $argumentsOrder, $qualifier)))
            else
                temp = $name
                setindex!($(name).genericFunctions, create_generic_function($((parameters...,)), $argumentsOrder, $qualifier), length($((parameters...,))))
                temp
            end
        ))
    end
end

# Initial macro for defining generics with user defined precedence order 
macro defgeneric(form::Expr, precedenceOrder::Expr, qualifier::Symbol = :standard)
    validate_generic_function(form)
    let name = form.args[1]
        esc(:($name = @defgeneric $form $(precedenceOrder.args) $qualifier))
    end
end

# Initial macro for defining generics without specifing the precedence order 
macro defgeneric(form::Expr, qualifier::Symbol = :standard)
    validate_generic_function(form)
    let name = form.args[1],
        parameters = form.args[2:end],
        precedenceOrder = vcat(:(:precedence), parameters)
        esc(:($name = @defgeneric $form $precedenceOrder $qualifier))
    end
end



# Macro to define a specific method
macro defmethod(qualifier::Symbol, form::Expr)
    validate_specific_method_form(form)
    let name = form.args[1].args[1],
        parameters = form.args[1].args[2:end],
        body = form.args[2]
        esc(:(
            let name = $(QuoteNode(name)),
                nParameters = length($(parameters)),
                parameterSignature = get_parameter_signature($(parameters)),
                qualifierObj = $(get_method_qualifier(qualifier)),
                qualifier = $(QuoteNode(qualifier))
                clean_cache($(name).genericFunctions[nParameters])
                let signature = Symbol(name, parameterSignature, :([$qualifier])),
                    specificMethod = create_specific_method($(name).genericFunctions[nParameters], qualifierObj, ($(parameters...),) -> $body)
                    setindex!($(name).genericFunctions[nParameters].methods, specificMethod, signature)
                end
            end
        ))
    end
end

macro defmethod(form)
    :@defmethod primary $form
end



# Method responsible for calling the effective method of a generic function
function call_effective_method(genericContainer::GenericContainer, arguments...)
    let genericFunction = genericContainer.genericFunctions[length(arguments)]
        call_effective_method(genericFunction, arguments...)
    end
end

# Method responsible for calling the effective method of a generic function with a specific number of arguments
function call_effective_method(genericFunction::GenericFunction, arguments...)
    let effectiveMethod = retrieve_cache_methods(genericFunction, arguments...)
        effectiveMethod(arguments...)
    end
end



# Method responsible for managing the cache of effectiveMethods
function retrieve_cache_methods(genericFunction::GenericFunction, arguments...)
    let signature = Symbol(map(p -> Symbol(typeof(p)), arguments))
        get(genericFunction.effectiveMethods, signature) do
            let effectiveMethod = combine_methods(genericFunction, genericFunction.qualifier, arguments...)
                setindex!(genericFunction.effectiveMethods, effectiveMethod, signature)
                return effectiveMethod
            end
        end
    end
end



# Main method responsible for getting the applicable methods and generating effective methods for a StandardCombination
function combine_methods(genericFunction::GenericFunction, qualifier::Qualifier, arguments...)
    let methods = find_methods(genericFunction, qualifier, arguments...)
        sort_and_generate_method(genericFunction, qualifier, methods, arguments...)
    end
end


# Method responsible for generating an effective method for a standard combination
function sort_and_generate_method(genericFunction::GenericFunction, qualifier::StandardQualifier, standardMethods::Dict, arguments...) 
    let beforeMethods = sort_methods(standardMethods[BeforeQualifier()], genericFunction.parametersOrder),
        primaryMethods = sort_methods(standardMethods[PrimaryQualifier()], genericFunction.parametersOrder),
        afterMethods = sort_methods(standardMethods[AfterQualifier()], genericFunction.parametersOrder, true)

        isempty(primaryMethods) ? (
            isempty(beforeMethods) && isempty(afterMethods) ?
                no_applicable_method(genericFunction, arguments...) :
                generate_standard_method(beforeMethods, afterMethods)
            ) :
            generate_standard_method(beforeMethods, first(primaryMethods), afterMethods)
    end
end

# Method responsible for generating an effective method for a tuple combination
function sort_and_generate_method(genericFunction::GenericFunction, qualifier::SimpleQualifier, tupleMethods::Vector{SpecificMethod}, arguments...) 
    let methods = sort_methods(tupleMethods, genericFunction.parametersOrder)
        isempty(methods) ?
            no_applicable_method(genericFunction, arguments...) :
            generate_simple_method(qualifier, methods)
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

function generate_simple_method(qualifier::SimpleQualifier, methods)
    (parameters...) -> begin
        simple_operation(qualifier)(map(m -> m.native_function(parameters...), methods))
    end
end



# Method responsible for retrieving the all three types of applicable methods of a StandardCombination separately
function find_methods(genericFunction::GenericFunction, qualifier::StandardQualifier, arguments...)
    function add_method_to_standard_list_if_applicable!(standardMethods::Dict, method::SpecificMethod, qualifier::Qualifier, arguments...)
        if method.qualifier == qualifier && applicable(method.native_function, arguments...)
            push!(standardMethods[qualifier], method)
        end
    end

    let standardMethods = Dict(BeforeQualifier() => [], PrimaryQualifier() => [], AfterQualifier() => [])
        foreach(m -> add_method_to_standard_list_if_applicable!(standardMethods, m, m.qualifier, arguments...), collect(values(genericFunction.methods)))
        return standardMethods
    end
end

# Method responsible for retrieving all applicable methods of a TupleCombination
function find_methods(genericFunction::GenericFunction, qualifier::SimpleQualifier, arguments...)
    filter(m -> applicable(m.native_function, arguments...), collect(values(genericFunction.methods)))
end



# Auxiliary methods used while generating the new combination method
function sort_methods(methods::Vector, order::Tuple, reverseOrder::Bool = false)
    function sort_predicate(A, B) 
        for i in order
            a = A[i]; b = B[i]
            a == b ? continue : return (a <: b)
        end
    end

    sort(methods, by = x -> x.parameters, lt = (x,y) -> sort_predicate(x, y), rev = reverseOrder)
end