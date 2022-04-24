# noargs methods
@defgeneric noargs()

@defmethod noargs() = 
    println("I got no args yeah")

# explain methods
@defgeneric explain(entity)

@defmethod explain(entity::Int) = print("$entity is a Int")

@defmethod explain(entity::Rational) = print("$entity is a Rational")

@defmethod explain(entity::String) = print("$entity is a String")

@defmethod after explain(entity::Number) = print(". This one is less specific.")

@defmethod after explain(entity::Int) = print(" (in binary, is $(string(entity, base=2)))")

@defmethod before explain(entity::Real) = print("The number ")

@defmethod before explain(entity::Int) = print("Most specific first! ")

@defmethod explain(entity::Number) = print(" and a Number ")

@defmethod explain(entity) = print("No type name")

# add methods
@defgeneric add(x,y)

@defmethod add(x::Int, y) = println("$x is an Int")

@defmethod add(x, y::Int) = println("$y is an Int")

@defmethod add(x::Int, y::Int) = x + y

@defmethod add(x::String, y::String) = x * y

@defmethod before add(x::Rational, y::Rational) = println("These are rationals")

# what methods (tuple)
@defgeneric what_are_you(entity) tuple

@defmethod what_are_you(n::Int64) = "I am a Int64"

@defmethod what_are_you(n::Float64) = "I am a Float64"

@defmethod what_are_you(n::Number) = "I am a Number"

@defmethod what_are_you(n::Rational) = "I am a Rational"

println("\n===== [Statement Test] =====")
what_are_you(1)
what_are_you(1234.534)
add(4,8)
explain(132)
add(6//4, 7//3)
explain(1.5454)
explain("54853")
noargs()

# generics with variadic number of arguments
println("\n===== [Generics with Variadic Number of Arguments Test] =====")
@defgeneric add(x, y, z)

@defmethod add(x::Int, y::Int, z::Int) = println(x + y + z)

add(1, 2, 3)

# arguments precedence order
println("\n===== [Arguments Precedence Order Test] =====")
@defgeneric no_precedence(x, y, z) tuple

@defmethod no_precedence(x::Int, y, z) = "$x is the first argument"

@defmethod no_precedence(x, y::Int, z) = "$y should be the last to be called"

@defmethod no_precedence(x, y, z::Int) = "$z should be the first to be called"

@defgeneric with_precedence(x, y, z) :precedence(z, x, y) tuple

@defmethod with_precedence(x::Int, y, z) = "$x is the first argument"

@defmethod with_precedence(x, y::Int, z) = "$y should be the last to be called"

@defmethod with_precedence(x, y, z::Int) = "$z should be the first to be called"

println(no_precedence(1,2,3))

println(with_precedence(1,2,3))