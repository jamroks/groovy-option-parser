import org.computoring.gop.Parser

description "Scenarios to validate the creation of options"

scenario "creating a single optional option", {
  given "a new parser", { parser = new Parser() }
  when "creating a single optional option with only a shortName", { parser.optional( 'f' ) }
  then "the parser should have single option", {
    parser.options.shouldHave( 'f' )
  }
  and "the option should not have default value", {
    parser.options.f.shouldNotHave( 'default' )
  }
}

scenario "creating an optional option including a longName", {
  given "a new parser", { parser = new Parser() }
  and "an optional option 'f' with long name 'foo'", {
    parser.optional( 'f', 'foo' )
  }
  then "options should contain 'f' and 'foo'", {
    ensure( parser.options ) {
      contains( 'f' )
      contains( 'foo' )
    }
  }
  and "option f should have type 'optional'", { parser.options.f.type.shouldBe( 'optional' ) }
  and "option f should not have a default value", {
    parser.options.f.default.shouldBe( null )
  }
  and "option f should = option foo", {
    parser.options.f.shouldEqual( parser.options.foo )
  }

  given "a new parser", { parser = new Parser() }
  when "creating an option with a hyphenated longName", { parser.optional( 'f', 'foo-bar' ) }
  then "the parser should have two options, f & foo-bar", {
    ensure( parser.options ) {
      contains( 'f' )
      contains( 'foo-bar' )
    }
  }
}

scenario "creating a flag option", {
  given "a new parser", { parser = new Parser() }
  when "creating a flag option", { parser.flag( 'f' ) }
  then "the parser should have an option 'f' with a default value of false", {
    parser.options.shouldHave( 'f' )
    parser.options.f.shouldHave( 'default' )
    parser.options.f.default.shouldBe( false )
  }
}

scenario "creating a flag option with a default value of true", {
  given "a new parser", { parser = new Parser() }
  when "a flag option is created with a default of true", { parser.flag( 'f', [default: true] )}
  then "the parser should have an option 'f' with a default of true", {
    parser.options.shouldHave( 'f' )
    parser.options.f.default.shouldBe( true )
  }
}

scenario "required options cannot have default values", {
  given "a new parser", { parser = new Parser() }
  when "a required option is created with a default value", {
    action = { parser.required( 'f', [default: 'bar']) }
  }
  then "an exception should be thrown", {
    ensureThrows( IllegalArgumentException.class ) { action() }
  }
}

scenario "duplicate options not allowed", {
  given "a new parser with an option 'f'", {
    parser = new Parser()
    parser.optional( 'f' )
  }
  when "creating an additional option 'f'", {
    action = { parser.required( 'f' ) }
  }
  then "an exception should be thrown", {
    ensureThrows( IllegalArgumentException.class ) { action() }
  }
}
