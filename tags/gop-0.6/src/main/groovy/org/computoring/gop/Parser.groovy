package org.computoring.gop

/**
 * Groovy Option Parser
 *
 * GOP is a command line option parser.  GOP is an alternative to CliBuilder.
 * 
 * An example:
 *  def parser = new org.computoring.gop.Parser(description: "An example parser.")
 *  parser.required('f', 'foo-bar', [description: 'The foo-bar option'])
 *  parser.optional('b', [longName: 'bar-baz', default: 'xyz', description: 'The optional bar-baz option with a default of "xyz"'])
 *  parser.flag('c')
 *  parser.flag('d', 'debug', [default: true])
 *  parser.required('i', 'count', [description: 'A required, validated option', validate: {
 *    Integer.parseInt(it)
 *  }])
 *                                                                                                                                  
 *  def params = parser.parse("-f foo_value --debug --count 123 -- some other stuff".split())
 *  assert params.'foo-bar' == 'foo_value'
 *  assert params.b == 'xyz'
 *  assert params.c == false
 *  assert params.debug == true
 *  assert params.count instanceof Integer
 *  assert params.i == 123
 *  assert parser.remainder.join(' ') == 'some other stuff'
 *
 */
public class Parser {

  /** A property describing this option parser.  Displayed in usage statement. */
  def description

  def options = [:]
  def parameters = [:]
  def remainder = []

  /**
   * Add a required option to the parser.
   * Parameters must be supplied for each required option at parsing time.
   *
   * @param shortName
   *        A single character name to use with for this option
   *
   * @param opts
   *        A map of additional options for this option.  Recognized options include:
   *        longName: String    -- A long name to use for this option.  Long names can be anything, but you
   *                               have to follow groovy rules of map key referencing, namely quoting anything
   *                               that isn't a simple string (i.e. params.'long-option')
   *        description: String -- A string describing this option.  This description will be used to create a
   *                               usage statement.
   *        validate: {Closure} -- A closure that will be passed the parameter supplied for this
   *                               option.  The return value of the closure is the final value of
   *                               the parameter.  This is useful for conversions and validations.
   */
  def required( String shortName, Map opts = [:] ) {
    if( opts.default ) {
      throw new IllegalArgumentException( "Default values don't make sense for required options" )
    }
    addOption( shortName, 'required', opts )
  }

  /**
   * A convienence method for required( shortName, [longName: name] ).
   * @see #required( String, Map )
   */
  def required( String shortName, String longName, Map opts = [:] ) {
    required( shortName, [longName: longName] + opts )
  }

  /**
   * Add an optional option to the parser.
   * Parameters are not required to be supplied for optional options at parsing time.  Additionally, optional
   * options may have a default value.
   *
   * @param shortName
   *        A single character name to use with for this option
   *
   * @param opts
   *        A map of additional options for this option.  Recognized options include:
   *        longName: String    -- A long name to use for this option.  Long names can be anything, but you
   *                               have to follow groovy rules of map key referencing, namely quoting anything
   *                               that isn't a simple string (i.e. params.'long-option')
   *        default: value      -- A default value to return if none is provided.  Note that the a default value
   *                               is processed by the validate closure is one is specified.
   *        description: String -- A string describing this option.  This description will be used to create a
   *                               usage statement.
   *        validate: {Closure} -- A closure that will be passed the parameter supplied for this
   *                               option.  The return value of the closure is the final value of
   *                               the parameter.  This is useful for conversions and validations.
   */
  def optional( String shortName, Map opts = [:] ) {
    addOption( shortName, 'optional', opts )
  }

  /**
   * A convienence method for optional( shortName, [longName: name] ).
   * @see #optional( String, Map )
   */
  def optional( String shortName, String longName, Map opts = [:] ) {
    optional( shortName, [longName: longName] + opts )
  }

  /**
   * Add a flag option to the parser.
   * Flags are boolean options that do not accept a value during parsing.  Flags are false by default and specifying
   * them during parsing will make them true.  Default value can be changed to true, see below.
   *
   * @param shortName
   *        A single character name to use with for this option
   *
   * @param opts
   *        A map of additional options for this option.  Recognized options include:
   *        longName: String    -- A long name to use for this option.  Long names can be anything, but you
   *                               have to follow groovy rules of map key referencing, namely quoting anything
   *                               that isn't a simple string (i.e. params.'long-option')
   *        default: value      -- The default value will be true or false depending on the 
   *                               truthiness of the supplied value.
   *        description: String -- A string describing this option.  This description will be used to create a
   *                               usage statement.
   *        validate: {Closure} -- A closure that will be passed the parameter supplied for this
   *                               option.  The return value of the closure is evaluated as true or false and assigned
   *                               to the parameter.
   */
  def flag( String shortName, Map opts = [:] ) {
    opts.default = ( opts.default ) ? true : false
    addOption( shortName, 'flag', opts )
  }

  /**
   * A convienence method for flag( shortName, [longName: name] ).
   * @see #flag( String, Map )
   */
  def flag( String shortName, String longName, Map opts = [:] ) {
    flag( shortName, [longName: longName] + opts )
  }

  /**
   * Apply configured options to the supplied args returning a map of parameters.
   * Each option that is mapped to a parameter is available in the returned map in its
   * short and optionally its long name.
   *
   * @param args
   *        Typically, an array of command line arguments.  Can be any Iterable. 
   *
   * @return Map
   *         A map of parsed parameters.  Each option that is mapped to a parameter will have an
   *         entry for its shortName and additionally for its longName if specified.
   */
  Map parse( args ) {
    // add defaults
    parameters = options.inject( [:] ) { map, entry ->
      if( entry.value.default != null ) map[entry.key] = entry.value.default
      map
    }

    def option = null
    args.each { arg ->
      // options can't look like -foo
      if( arg =~ ~/^-[^-].+/ ) {
        throw new IllegalArgumentException( "Illegal option [$arg], short options must be a single character" )
      }

      if( arg =~ ~/^(-[^-]|--.+)$/ ) {
        if( option ) {
          throw new IllegalArgumentException( "Illegal value [$arg] supplied for option ${option.shortName}" )
        }

        def name = arg.replaceFirst( /--?/, '' )
        if( !options.containsKey( name )) {
          throw new Exception( "unknown option $arg" )
        }

        option = options[name]
        if( option.type == 'flag' ) {
          addParameter(option, true)
          option = null
        }
      }
      else if( option ) {
        addParameter(option, arg)
        option = null
      }
      else {
        if( !( arg == '--' )) remainder << arg
      }
    }

    def missing = ( requiredOptions.keySet() - parameters.keySet() )
    if( missing ) throw new Exception( "Missing required parameters: ${missing.collect { "-$it" }}" )

    return parameters
  }

  /**
   * Returns a formatted String describing this parser's options with their default values and
   * descriptions.
   *
   * Note that effort is made to align defaults and descriptinos vertically.  This can be a bit
   * wonky if you supply a large default or description.
   *
   * @param errorMsg
   *        When supplied, errorMsg will be displayed at the beginning of the usage message.
   *        Useful for reporting exceptions during parsing or values that fail option validation.
   */
  String usage( errorMsg = null ) {
    def buffer = new StringWriter()
    def writer = new PrintWriter( buffer )

    if( errorMsg ) {
      writer.println( "Error: $errorMsg" )
      writer.println()
    }

    if( description ) {
      writer.println( description )
      writer.println()
    }

    def longestName = 5 + options.inject( 0 ) { max, entry -> 
      entry.value.longName ? Math.max( max, entry.value.longName.size() ) : max
    }

    def longestDefault = 5 + options.inject( 0 ) { max, entry -> 
      def x = entry.value.default
      (x && x.metaClass.respondsTo(x, "size")) ? Math.max( max, x.size() ) : max
    }

    def pattern = "%s%-${longestName}s %-${longestDefault}s %s"
    ['Required': requiredOptions, 'Optional': optionalOptions, 'Flags': flagOptions].each { header, map ->
      if( map ) {
        writer.println( header )
        writer.println( "-"*header.size() )
        map.each { name, opts ->
          def shortName = "-$opts.shortName"
          def longName = opts.longName ? ", --$opts.longName" : ""
          def defaultValue = (opts.default || opts.type == 'flag') ? "[${opts.default.toString()}]" : ""
          def description = opts.description ?: ""
          writer.printf( pattern, shortName, longName, defaultValue, description)
          writer.println()
        }
      }

      writer.println()
    }

    return buffer.toString()
  }

  private def addParameter(option, value) {
    if( option.validate ) {
      value = option.validate( value )
      if( option.type == 'flag' ) value = value ? true : false
    }
    parameters[option.shortName] = value
    if( option.longName ) parameters[option.longName] = value
  }

  private def getRequiredOptions() {
    findOptions( 'required' )
  }

  private def getOptionalOptions() {
    findOptions( 'optional' )
  }

  private def getFlagOptions() {
    findOptions( 'flag' )
  }

  private def findOptions( type ) {
    options.inject( [:] ) { map, entry ->
      if( entry.value.type == type ) map[entry.value.shortName] = entry.value
      map
    }
  }

  private def addOption( shortName, type, opts ) {
    opts = opts ?: [:]

    if( !shortName ) {
      throw new IllegalArgumentException( "Option name cannot not be null" )
    }

    if( options[shortName] ) {
      throw new IllegalArgumentException( "Dup option specified: $shortName" )
    }

    if( shortName.size() != 1 ) {
      throw new IllegalArgumentException( "Invalid option name: $shortName.  Option names must be a single character.  To set a long name for this option add [longName: 'long-name']" )
    }

    if( opts.validate && !(opts.validate instanceof Closure) ) {
      throw new IllegalArgumentException( "Invalid validate option, must be a Closure" )
    }

    opts.type = type ?: 'optional'
    opts.shortName = shortName
    options[shortName] = opts
    if( opts.longName ) options[opts.longName] = opts
  }

  private setOptions( arg ) {}
}

