# Groovy Option Parser #

GOP is a command line option parsing alternative to CliBuilder.

I wrote GOP for a couple reasons:
  * Needed a small project to learn groovy
  * Replicate the option parsing behaviour of the excellent Ruby library, [clip](http://github.com/alexvollmer/clip/tree/master.).  Clip makes it simple to define options and simple to use the parsed parameters.
  * CliBuilder is confusing and made me think too hard

## An example is the easiest explanation ##
```
 def parser = new org.computoring.gop.Parser(description: "An example parser demonstrating the features and uses of GOP.")
 parser.with {
   // support for required options
   // required options cannot have default values, that doesn't make much sense
   required 'f', 'foo-bar', [description: 'A required option with a short name, a long name, and a description'] 

   // support for optional options
   optional 'b', [
     longName: 'bar-baz',   // longName can be specified this way also
     default: 'xyz', 
     description: 'An optional option with a short name, a long name, a default value, and a description'
   ]

   // support for flag (boolean) options
   flag 'c'  // a flag option without a long name or a description, flags default to false
   flag 'd', 'debug', [
     default: true,
     description: 'A flag option with a default value of true'
   ]

   // short names are not required, pass in null
   optional null, 'optional-long-opt', [ description: 'An optional option without a shortname.' ]
   required null, 'required-long-opt', [ description: 'A required option without a shortname.' ]

   // support for parameter validation.  The parameter is passed to the validation closure.  The value
   // returned from the closure is assigned back to the option.
   required 'i', 'count', [
     description: 'A required, validated option', 
     validate: {
       Integer.parseInt it  // the value of the parsed option with be an Integer in this case
     }
   ] 

   // Support for remainder validation.  Whatever is remaining after the options are parsed is passed into
   // the remainder closure.  Whatever is returned from the closure is available as parser.remainder.
   // In this case a command is required after parameters.
   remainder {
     if(!it) throw new IllegalArgumentException("missing command")
     it
   }
 }

 // typically, I'll call parse like this.  If anything blows up I catch Exception and 
 // print the parser.usage to stderr and exit
 try {
   // A script at this point would call parse with the command line args, 
   //   def params = parser.parse(args)
   // '--' stops option parsing
   def params = parser.parse("-f foo_value --debug --count 123 --required-long-opt wookie -- --not-an-option some other stuff".split())

   // parsed as -f, referenced here as params.'foo-bar'
   assert params.'foo-bar' == 'foo_value'

   // -b not supplied, 'xyz' is the default value
   assert params.b == 'xyz'

   // -c not supplied, flag options default to false
   assert params.c == false

   // --debug was supplied and flag option set to true
   assert params.debug == true

   // -i and --count validate and convert their parsed value into an Integer
   assert params.count instanceof Integer
   assert params.i == 123

   assert params.'required-long-opt' == 'wookie'

   // verify the remainder contains everything after '--'
   assert parser.remainder.join(' ') == '--not-an-option some other stuff'
 }
 catch( Exception e ) {
   System.err << parser.usage
   System.exit(1)
 }
```

The example above will generate usage output like this
```
An example parser demonstrating the features and uses of GOP.
Required
  -f, --foo-bar                       A required option with a short name, a long name, and a description
      --required-long-opt             A required option without a shortname.
  -i, --count                         A required, validated option

Optional
  -b, --bar-baz              [xyz]    An optional option with a short name, a long name, a default value, and a description
      --optional-long-opt             An optional option without a shortname.

Flags
  -c                         [false]  
  -d, --debug                [true]   A flag option with a default value of true
```


---


## A couple interesting features ##
  * Flag and optional options can have default values
  * Options can specify a validation closure that parsed values are passed through
  * remaining parameters after parsing can be validated
  * Formatted usage statement is created for you.

  * Homepage:      http://code.google.com/p/groovy-option-parser/
  * Downloads:     http://code.google.com/p/groovy-option-parser/downloads/list
  * Issue tracker: http://code.google.com/p/groovy-option-parser/issues/list
