# noargs methods
@defgeneric noargs()

@defmethod noargs() = println("I got no args yeah")

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
