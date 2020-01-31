var current;

/**
    Chucklib-livecode: A framework for live-coding improvisation of electronic music
    Copyright (C) 2018  Henry James Harkins

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
**/

Proto {
	~prep = { |value|  // anything that .asStream's and .next's to a number
		~val = value;
		~reset.();
		currentEnvironment
	};
	~reset = {
		~valStream = ~val.asStream;
		currentEnvironment
	};
	~next = { |inval|
		var out = ~valStream.next(inval);
		if(out.notNil) {
			out
		} {
			~reset.();
			~valStream.next(inval)
		};
	};
	// acts as its own stream
	// it's not a pattern, so it doesn't need stream independence
	// users shouldn't use this object directly as a pattern
	~asStream = { currentEnvironment };
	~isNumProxy = true;
} => PR(\clNumProxy);

Proto {
	~begin = nil;
	~end = nil;
	~selectionStart = { ~begin };
	~selectionSize = { ~end - ~begin + 1 };
	~siblings = {
		if(~parentNode.notNil) { ~parentNode.children };  // else nil
	};
	~index = {
		var sibs = ~siblings.();
		if(sibs.notNil) { sibs.indexOf(currentEnvironment) };  // else nil
	};
	~nearbySib = { |incr = 1|
		var sibs = ~siblings.(), i;
		if(sibs.notNil) {
			i = sibs.indexOf(currentEnvironment);
			if(i.notNil) {
				sibs[i + incr]  // may be nil
			}
		};  // else nil
	};
	~setLastSelected = {
		var i = ~index.();
		if(i.notNil) { ~parentNode.tryPerform(\lastSelected_, i) };
		currentEnvironment
	};

	~prep = { |stream, parentNode|
		~parentNode = parentNode;
		~children = Array.new;
		// if(stream.peek == $/) { stream.next };
		~begin = stream.pos;
		~parse.(stream);
		if(~end.isNil) { ~end = stream.pos - 1 };
		if(~string.isNil) {
			if(~end >= ~begin) {
				~string = stream.collection[~begin .. ~end]
			} {
				~string = String.new;
			};
		};
		currentEnvironment
	};

	// utility function
	~unquoteString = { |str, pos = 0, delimiter = $", ignoreInParens(false)|
		var i = str.indexOf(delimiter), j, escaped = false, parenCount = 0;
		if(i.isNil) {
			str
		} {
			j = i;
			while {
				j = j + 1;
				j < str.size and: {
					escaped or: { str[j] != delimiter }
				}
			} {
				switch(str[j])
				{ $\\ } { escaped = escaped.not }
				{ $( } {
					if(ignoreInParens) {
						parenCount = parenCount + 1;
						escaped = true;
					} {
						escaped = false;
					};
				}
				{ $) } {
					if(ignoreInParens) {
						parenCount = parenCount - 1;
						if(parenCount < 0) {
							"unquoteString: paren mismatch in '%'".format(str).warn;
						} {
							escaped = parenCount > 0;
						};
					} {
						escaped = false;
					};
				}
				{
					if(ignoreInParens.not or: { parenCount <= 0 }) {
						escaped = false;
					};
				}
				// if(str[j] == $\\) { escaped = escaped.not } { escaped = false };
			};
			if(j - i <= 1) {
				String.new  // special case: two adjacent quotes = empty string
			} {
				str[i + 1 .. j - 1];
			};
		};
	};

	~idAllowed = "_";
	~getID = { |stream, skipSpaces(true)|
		var str = String.new, ch, begin;
		if(skipSpaces) { ~skipSpaces.(stream) };
		begin = stream.pos;
		while {
			ch = stream.next;
			ch.notNil and: { ch.isAlphaNum or: { ~idAllowed.includes(ch) } }
		} {
			str = str.add(ch);
		};
		if(ch.notNil) { stream.pos = stream.pos - 1 };
		PR(\clStringNode).copy
		.put(\parentNode, currentEnvironment)
		.put(\string, str)
		.put(\begin, begin)
		.put(\end, begin + str.size - 1);
	};
	~skipSpaces = { |stream|
		var ch;
		while {
			ch = stream.next;
			ch.notNil and: { ch.isSpace }
		};
		if(ch.notNil) { stream.pos = stream.pos - 1 };
	};

	~streamCode = { |stream| stream << ~string; currentEnvironment };
	~setTime = { |onset(0), dur(4)|
		~time = onset;
		~dur = dur;
		currentEnvironment
	};
	~isSpacer = false;
} => PR(\abstractClParseNode);

PR(\abstractClParseNode).clone {
	// assumes you've skipped the opening delimiter
	~parse = { |stream|
		var str = String.new, ch;
		while {
			ch = stream.next;
			ch.notNil and: { ~endTest.(ch).not }
		} {
			str = str.add(ch);
		};
		if(ch.notNil) { stream.pos = stream.pos - 1 };
		str
	};
	~endTest = { |ch| ch == $\" };
	~symbol = { ~string.asSymbol };
	~openDelimiter = "";
	~closeDelimiter = "";
	~streamCode = { |stream|
		stream << ~openDelimiter << ~string << ~closeDelimiter;
		currentEnvironment
	};
	~isSpacer = { ~string.every(_.isSpace) };
} => PR(\clStringNode);

PR(\clStringNode).clone {
	~streamCode = { |stream|
		stream << "(time: " << ~time << ", dur: " << ~dur << ", item: ";
		if(~isPitch ?? { false }) {
			stream <<< ~decodePitch.(~string);
		} {
			if(~string.size == 1) {
				stream <<< ~string[0];
			} {
				stream <<< ~string;
			};
		};
		stream << ")";
		currentEnvironment
	};
}.import((clPatternSet: #[decodePitch])) => PR(\clEventStringNode);

PR(\clStringNode).clone {
	~artic = "_.~";
	~accent = ">";
	~parse = { |stream|
		var ch;
		ch = stream.next;
		if(~accent.includes(ch)) {
			~accented = true;
			ch = stream.next;
			~checkArtic.(ch, stream);
		} {
			~checkArtic.(ch, stream);
		};
		currentEnvironment
	};
	~checkArtic = { |ch, stream|
		case
		{ ~artic.includes(ch) } {
			~char = ch;
			ch
		}
		{ ~accent.includes(ch) or: { ch == $" } } {
			~char = nil;  // special case (also valid for terminating quote)
			stream.pos = stream.pos - 1;  // >~ is 2 chars; >> is two things of one char each
		}
		{
			Error("Articulation pool: Bad character $%".format(ch)).throw;
		}
	};
	~streamCode = { |stream|
		if(~accented == true) {
			stream << "\\accent -> " <<< ~char;
		} {
			stream <<< ~char;
		};
		currentEnvironment
	};
} => PR(\clArticNode);

PR(\clStringNode).clone {
	~parse = { |stream|
		var ch;
		while {
			ch = stream.peek;
			ch.notNil and: { ~endTest.(ch).not }
		} {
			~children = ~children.add(PR(\clArticNode).copy.prep(stream));
		};
		stream.next;  // eat the last quote
		~children
	};
	~streamCode = { |stream|
		stream << "[";
		~children.do { |item, i|
			if(i > 0) { stream << ", " };
			item.streamCode(stream);
		};
		stream << "]";
		currentEnvironment
	};
} => PR(\clArticPoolNode);

PR(\abstractClParseNode).clone {
	~types = [
		// fraction: integer/integer
		{ |str|
			var slash = str.indexOf($/),  // regexp guarantees "/" is there
			numerator = str[ .. slash-1].asInteger,
			denominator = str[slash+1 .. ].asInteger;
			Rational(numerator / denominator)
		} -> "[\\-+]?[0-9]+/[0-9]+",
		// tuplet: numNotes:noteValue
		{ |str|
			var colon = str.indexOf($:),  // regexp guarantees ":" is there
			numerator = str[ .. colon-1].asFloat,
			denominator = str[colon+1 .. ].asInteger;
			// 3:2 = triplet spanning half note
			// 3:4 = triplet spanning quarter note
			// 5:4 = quintuplet spanning quarter
			// 4:1 = 4 in a whole-note = quarter note
			denominator * 0.25 / numerator  // was Rational(), needed?
		} -> "[\\-+]?[0-9]*\\.?[0-9]+([eE][\\-+]?[0-9]+)?:[0-9]+",
		_.asFloat -> "[\\-+]?[0-9]*\\.[0-9]+([eE][\\-+]?[0-9]+)?",
		_.asInteger -> "(-[0-9]+|[0-9]+)"
	];
	~parse = { |stream|
		// hack into CollStream -- will fail with other IO streams
		var match,
		type = ~types.detect { |assn|
			match = stream.collection.findRegexpAt(assn.value, stream.pos);
			match.notNil
		};
		if(match.notNil) {
			~string = stream.nextN(match[1]);  // match[1] = length of match
			~value = type.key.value(~string);
		};  // else leave state variables nil
	};
	~streamCode = { |stream|
		stream << "PR(\\clNumProxy).copy.prep(";
		if(~value.isKindOf(Rational)) {
			stream <<< ~value.numerator << "/" <<< ~value.denominator;
		} {
			stream <<< ~value;
		};
		stream << ")";
		currentEnvironment
	};
	// Proto:value redirects here
	~next = { ~value };
} => PR(\clNumberNode);

PR(\clNumberNode).clone {
	~parse = { |stream|
		var low, hi;
		low = PR(\clNumberNode).copy.prep(stream);
		if(low.value.notNil) {
			~skipSpaces.(stream);
			2.do {
				if(stream.next != $.) {
					Error("Invalid range separator").throw;
				}
			};
			~skipSpaces.(stream);
			hi = PR(\clNumberNode).copy.prep(stream);
			if(hi.value.isNil) {
				Error("No upper bound found in range").throw;
			};
		} {
			Error("Invalid lower bound in range").throw;
		};
		if(low.value <= hi.value) {
			~children = [low, hi];
		} {
			~children = [hi, low];
		};
		currentEnvironment
	};
	~streamCode = { |stream|
		stream << "PR(\\clNumProxy).copy.prep(Pwhite("
		<<< ~children[0].value << ", " <<< ~children[1].value << ", inf))";
		currentEnvironment
	};
} => PR(\clRangeNode);

// \ins(, ".", {#[1, 4].choose}, 0.25)
// inside curly braces, brackets and quotes are allowed; SC comments aren't
PR(\clStringNode).clone {
	~parse = { |stream|
		var start, ch;
		if(stream.next != ${) {
			Error("Tried to parse pass-through expression not starting with a brace, should never happen")
			.throw;
		};
		start = stream.pos;  // bad interface in the \clParse helper functions
		while {
			ch = stream.next;
			ch.notNil and: { ch != $} }
		} {
			case
			{ "([{".includes(ch) } {
				ch = \clParseBracketed.eval(stream, ch, false);
			}
			{ "\'\"".includes(ch) } {
				ch = \clParseQuote.eval(stream, ch);
			};
			// else keep going by single chars
		};
		if(ch.isNil) {
			Error("Unterminated pass-through expression: %".format(
				stream.collection[start .. start + 10]
			)).throw;
		};
		~value = stream.collection[start .. stream.pos - 2];
	};
	~next = { ~value };
	~streamCode = { |stream|
		// trickier than you think.
		// non-patterns --> Pfunc({ expr }); patterns --> no Pfunc.
		// we have to evaluate the expression to know which is which.
		// avoid code duplication in the generated string by using a function.
		stream << "PR(\\clNumProxy).copy.prep(Plazy { var func = { " << ~value
		<< " }, thing = func.value; if(thing.isPattern) { PnNilSafe(thing, inf, 10) } { Pfunc(func) } })";
		currentEnvironment
	};
} => PR(\clPassthruNumberNode);

PR(\abstractClParseNode).clone {
	~isPitch = false;
	~endChars = "|\"";
	~parse = { |stream|
		var ch;
		// stream.collection[stream.pos .. stream.pos + 10].debug(">> clDividerNode");
		~items = Array.new;
		while {
			ch = stream.next;
			ch.notNil and: { ~endChars.includes(ch).not }
		} {
			~items = ~items.add(~parseItem.(stream, ch));
		};
		if(ch.notNil) { stream.pos = stream.pos - 1 };
		// stream.collection[~begin .. ~begin + 10].debug("<< clDividerNode");
	};

	~parseItem = { |stream, ch|
		var new, begin;
		// [ch.asCompileString, stream.collection[stream.pos .. stream.pos + 10]].debug(">> parseItem");
		if(ch.isNil) { ch = stream.next };
		begin = stream.pos - 1;
		case
		{ ch == $\\ } {
			stream.pos = stream.pos - 1;
			new = PR(\clGeneratorNode).copy
			.putAll((bpKey: ~bpKey, isPitch: ~isPitch, isMain: ~isMain, parm: ~parm))
			.prep(stream, currentEnvironment);
			new = ~handleChain.(stream, new);
			~children = ~children.add(new);
			new//.debug("<< parseItem");
		}
		{ ch == $[ } {
			stream.pos = stream.pos - 1;
			new = PR(\clSourceNode).copy
			.putAll((bpKey: ~bpKey, isPitch: ~isPitch, isMain: ~isMain, parm: ~parm))
			.prep(stream, currentEnvironment);
			new = ~handleChain.(stream, new);
			~children = ~children.add(new);
			new//.debug("<< parseItem");
		}
		// legit chains should be handled in one of the above branches
		{ stream.collection[stream.pos .. stream.pos + 2] == "::\\" } {
			Error(":: chain syntax applies only to generators or [source]").throw;
		}
		{ ~isPitch } {
			if(ch.isDecDigit) {
				new = String.with(ch);
				while { (ch = stream.next).notNil and: { "+-',~_.>".includes(ch) } } {
					new = new.add(ch);
				};
				if(ch.notNil) { stream.pos = stream.pos - 1 };
				~children = ~children.add(
					PR(\clEventStringNode).copy
					.put(\parentNode, currentEnvironment)
					.put(\string, new)
					.put(\isPitch, ~isPitch)
					.put(\begin, begin)
					.put(\end, begin + new.size - 1);
				);
				new//.debug("<< parseItem");
			} {
				new = String.with(ch);
				~children = ~children.add(
					PR(\clEventStringNode).copy
					.put(\parentNode, currentEnvironment)
					.put(\string, new)
					.put(\isPitch, ~isPitch)
					.put(\begin, begin)
					.put(\end, begin);  // one char only
				);
				new//.debug("<< parseItem");
			};
		} {
			new = String.with(ch);
			~children = ~children.add(
				PR(\clEventStringNode).copy
				.put(\parentNode, currentEnvironment)
				.put(\string, new)
				.put(\isPitch, ~isPitch)
				.put(\begin, begin)
				.put(\end, begin);
			);
			new//.debug("<< parseItem");
		};
	};

	~handleChain = { |stream, new|
		var begin, colonCount, rewindPos;
		// if(#[clGeneratorNode, clSourceNode].includes(new.nodeType.debug("nodeType")).not) {
		// 	Error(":: chain syntax applies only to generators or [source]").throw;
		// };
		if(stream.peek == $:) {
			colonCount = 0;
			rewindPos = stream.pos;
			while { stream.next == $: } { colonCount = colonCount + 1 };
			if(colonCount == 2) {
				stream.pos = stream.pos - 1;  // need next to be the backslash
				new = PR(\clChainNode).copy
				.putAll((bpKey: ~bpKey, isPitch: ~isPitch, isMain: ~isMain, parm: ~parm))
				.prep(stream, currentEnvironment, new);  // 3rd arg is first item in the chain
			} {
				Error("'::' syntax, wrong number of colons").throw;
			};
		};
		new
	};

	~hasItems = { ~children.any { |item| item.isSpacer.not } };
	~streamCode = { |stream|
		var needComma = false;
		if(~hasItems.()) {
			if(~children[0][\time].isNil) { ~setTime.(~time, ~dur) };
			// no array brackets: divider delimiters are for humans, not machines
			~children.do { |item, i|
				if(item.isSpacer.not) {
					if(needComma) { stream << ", " };
					item.streamCode(stream);
					needComma = true;
				};
			};
		};
		currentEnvironment
	};
	~setTime = { |onset(0), dur(4), extraDur(0)|
		var itemDur = dur / max(~children.size, 1), durs, lastI;
		~time = onset;
		~dur = dur;
		// children should be clStringNodes or clGeneratorNodes
		durs = Array(~children.size);
		~children.do { |item, i|
			if(item.isSpacer and: { lastI.notNil }) {
				durs[lastI] = durs[lastI] + itemDur;
			} {
				lastI = i;
			};
			durs.add(itemDur);
		};
		if(lastI.notNil) {
			durs[lastI] = durs[lastI] + extraDur;
		};
		~children.do { |item, i|
			item.setTime(onset + (i * itemDur), durs[i]);
		};
	};
} => PR(\clDividerNode);

PR(\abstractClParseNode).clone {
	~isPitch = false;
	~types = [
		(type: \clRhythmGenNode, regexp: "^:[a-zA-Z0-9_]+\\(.*\\)"),
		(type: \clGeneratorNode, regexp: "^\\\\[a-zA-Z0-9_]+"),
		(type: \clPassthruNumberNode, regexp: "^\{.*\}"),
		// more specific "" test must come first
		(type: \clArticPoolNode, regexp: "^\"[._~>]+\"",
			match: { ~isPitch ?? { false } },
			pre: { |stream| stream.next }  // stringnodes assume you've already dropped the opening quote
		),
		(type: \clPatStringNode, regexp: "^\".*\""),
		(type: \clRangeNode, regexp: "^-?[0-9.]+ *\\.\\. *-?[0-9]"),
		(type: \clNumberNode, regexp: "^-?[0-9]"),
		(type: \clStringNode, regexp: "^`[A-Za-z0-9_]+",
			endTest: { |ch| not(ch.isAlphaNum or: { "_`".includes(ch) }) },
			openDelimiter: $', closeDelimiter: $',
			pre: { |stream| stream.next }  // drop ` intro
		),
		(type: \clChainNode, regexp: "^::",
			pre: { |stream| stream.nextN(2) },  // drop ::
			// chain node needs to get the first sub-generator at prep time
			// also the chain replaces the last-parsed sub-generator
			parseSpecial: { |new, stream|
				if(~children.last.nodeType == \clGeneratorNode) {
					new.prep(stream, currentEnvironment, ~children.last);
					~children = ~children.drop(-1);  // last is subsumed into 'new'
					new
				} {
					Error("'::' syntax is valid between two generators only").throw;
				};
			}
		),
		(type: \clStringNode, regexp: "^,",  // empty arg
			endTest: { |ch| ch == $, }
		),
	];
	~extras = #[endTest, openDelimiter, closeDelimiter];
	~parse = { |stream|
		var name, ch, newArg, testName;
		// stream.collection[stream.pos .. stream.pos + 10].debug(">> clGeneratorNode");
		if(stream.peek == $\\) { stream.next };
		name = ~getID.(stream);
		testName = name.string.copy;
		testName[0] = testName[0].toUpper;
		if(PR.exists(("clGen" ++ testName).asSymbol).not) {
			Error("Incorrect generator name %".format(name.string)).throw;
		};
		~children = ~children.add(name);
		~name = name.string;
		ch = stream.next;
		if(ch == $*) {
			case
			{ ~findType.(\clRangeNode)[\regexp].matchRegexp(stream.collection, stream.pos) } {
				~repeats = PR(\clRangeNode).copy.prep(stream, currentEnvironment);
			}
			{ ~findType.(\clNumberNode)[\regexp].matchRegexp(stream.collection, stream.pos) } {
				~repeats = PR(\clNumberNode).copy.prep(stream, currentEnvironment);
			};
			ch = stream.next;
		};
		if(ch == $() {
			while {
				ch = stream.next;
				ch.notNil and: { ch != $) }
			} {
				stream.pos = stream.pos - 1;
				// note: it is now not valid to collapse to ~children.add(~parseArg.(stream))
				// because ~parseArg will modify ~children if it encounters a chain node
				// ~parseArg must finish before determining the receiver of 'add'
				newArg = ~parseArg.(stream);
				~children = ~children.add(newArg);
			};
			// if(ch.notNil) { stream.pos = stream.pos - 1 };
		} {
			// gen refs
			Error("Generator '%' has no argument list".format(~name)).throw;
		};
		// stream.collection[~begin .. ~begin + 10].debug("<< clGeneratorNode");
	};
	~parseArg = { |stream|
		var type, ch, new;
		// stream.collection[stream.pos .. stream.pos + 10].debug(">> parseArg");
		type = ~types.detect { |entry|
			(entry[\match].value ?? { true }) and: {
				entry[\regexp].matchRegexp(stream.collection, stream.pos)
			}
		};
		if(type/*.debug("type")*/.notNil) {
			type[\pre].value(stream);
			new = PR(type.type).copy
			.putAll((bpKey: ~bpKey, isPitch: ~isPitch, isMain: ~isMain, parm: ~parm));
			~extras.do { |key|
				if(type[key].notNil) {
					new.put(key, type[key]);
				};
			};
			if(type[\parseSpecial].notNil) {
				type[\parseSpecial].value(new, stream);
			} {
				new.prep(stream, currentEnvironment);
			};
		} {
			Error("Syntax error in % arg list, at '%'".format(~name, stream.collection[stream.pos .. stream.pos + 10])).throw;
		};
		ch = stream.next;
		if(ch == $,) { ~skipSpaces.(stream) } {
			if(ch.notNil) { stream.pos = stream.pos - 1 };
		};
		// new.listVars; "<< parseArg".debug;
		new
	};

	~streamCode = { |stream|
		var name = ~children[0].string;
		// if(~children[1].isNil or: { ~children[1][\time].isNil }) {
			~setTime.(~time, ~dur);
		// };
		stream << "PR(\\clGen";
		if(name.size > 0) {
			stream << name[0].toUpper << name[1..];
		};
		stream << ").copy.putAll((";
		if(~repeats.notNil) {
			stream << "repeats: ";
			~repeats.streamCode(stream);
			stream << ", ";
		};
		stream << "bpKey: " <<< ~bpKey;
		stream << ", args: [ ";
		forBy(1, ~children.size - 1, 1) { |i|
			if(i > 1) { stream << ", " };
			if(~children[i].string == "") {
				stream << "nil"
			} {
				~children[i].streamCode(stream);
			};
		};
		stream << " ], dur: " << ~dur << ", time: " << ~time;
		stream << ", isPitch: " << (~isPitch ?? { false });
		stream << ", isMain: " << (~isMain ?? { false });
		stream << ", parm: " <<< ~parm;
		stream << ")).prep";
		currentEnvironment
	};
	~setTime = { |onset(0), dur(4)|
		var itemDur = dur / max(~children.size, 1);
		~time = onset;
		~dur = dur;
		forBy(1, ~children.size - 1, 1) { |i|
			~children[i].setTime(onset, dur);  // meaningful for gens and patstrings
		};
		currentEnvironment
	};

	~findType = { |key|
		~types.detect { |type| type[\type] == key }
	};
} => PR(\clGeneratorNode);

PR(\abstractClParseNode).clone {
	// special constructor -- you know what kind of node you're creating
	// assumes stream.next will be the second generator
	~prep = { |stream, parentNode, leftNode|
		if(stream.peek != $\\) {
			Error("'::' syntax is valid between two generators only").throw;
		};
		~parentNode = parentNode;
		leftNode.parentNode = currentEnvironment;
		~children = [leftNode];
		// if(stream.peek == $/) { stream.next };
		~begin = leftNode.begin; // stream.pos;
		~parse.(stream);
		if(~end.isNil) { ~end = stream.pos - 1 };
		if(~string.isNil) {
			if(~end >= ~begin) {
				~string = stream.collection[~begin .. ~end]
			} {
				~string = String.new;
			};
		};
		currentEnvironment
	};

	~parse = { |stream|
		var new, continue = true, rewindPos, colonCount;
		// stream.collection[stream.pos .. stream.pos + 10].debug(">> clChainNode");
		while { continue } {
			if(stream.peek == $\\) {
				new = PR(\clGeneratorNode).copy
				.putAll((bpKey: ~bpKey, isPitch: ~isPitch, isMain: ~isMain, parm: ~parm))
				.prep(stream, currentEnvironment);
				~children = ~children.add(new);
				if(stream.peek == $:) {
					rewindPos = stream.pos;
					colonCount = 0;
					while { stream.next == $: } { colonCount = colonCount + 1 };
					switch(colonCount)
					{ 2 } {
						stream.pos = stream.pos - 1;
					}
					{ 0 } {
						stream.pos = rewindPos;
						continue = false;
					}
					{ Error("'::' syntax, wrong number of colons").throw };
					if(stream.peek != $\\) {
						Error("'::' syntax is valid between two generators only").throw;
					};
				} {
					continue = false;
				};
			} {
				continue = false;
			}
		};
		// stream.collection[stream.pos .. stream.pos + 10].debug("<< clChainNode");
	};
	~streamCode = { |stream|
		if(~children[0][\time].isNil) { ~setTime.(~time, ~dur) };
		stream << "PR(\\clGenChain).copy.putAll((";
		stream << "bpKey: " <<< ~bpKey;
		stream << ", args: [ ";
		~children.do { |child, i|
			if(i > 0) { stream << ", " };
			child.streamCode(stream);
		};
		stream << " ], dur: " << ~dur << ", time: " << ~time;
		stream << ", isPitch: " << (~isPitch ?? { false });
		stream << ", isMain: " << (~isMain ?? { false });
		stream << ", parm: " <<< ~parm;
		stream << ")).prep";
		currentEnvironment
	};
	~setTime = { |onset(0), dur(4)|
		var itemDur = dur / max(~children.size, 1), i, extraDur = 0, first;
		~time = onset;
		~dur = dur;
		~children.do { |item| item.setTime(onset, dur) };
		currentEnvironment
	};
} => PR(\clChainNode);

PR(\clGeneratorNode).clone {
	~superParse = ~parse;
	~parse = { |stream|
		if(stream.peek == $:) { stream.next };
		~superParse.(stream);
	};
} => PR(\clRhythmGenNode);

PR(\abstractClParseNode).clone {
	~isPitch = false;
	~parse = { |stream|
		var str = String.new, ch, didOpenQuote = false;
		// stream.collection[stream.pos .. stream.pos + 10].debug(">> clPatStringNode");
		while {
			ch = stream.next;
			ch.notNil and: { didOpenQuote.not or: { ch != $\" } }
		} {
			~children = ~children.add(
				PR(\clDividerNode).copy
				.putAll((bpKey: ~bpKey, isPitch: ~isPitch, isMain: ~isMain, parm: ~parm))
				.prep(stream, currentEnvironment)
			);
			didOpenQuote = true;
		};
		// stream.collection[~begin .. ~begin + 10].debug("<< clPatStringNode");
	};
	~streamCode = { |stream|
		var needComma = false;
		if(~children[0][\time].isNil) { ~setTime.(~time, ~dur) };
		stream << "[ ";
		~children.do { |item, i|
			if(item.hasItems) {  // all items should be divider nodes
				if(needComma) { stream << ", " };
				item.streamCode(stream);
				needComma = true;
			};
		};
		stream << " ]";
		currentEnvironment
	};
	~setTime = { |onset(0), dur(4)|
		var itemDur = dur / max(~children.size, 1), i, extraDur = 0, first;
		~time = onset;
		~dur = dur;
		// all children should be divider nodes
		// what about items spanning a division? reverse order
		i = ~children.size - 1;
		~children.reverseDo { |item|
			var itemOnset = onset + (i * itemDur);
			item.setTime(itemOnset, itemDur, extraDur);
			if(item.children.size == 0) {
				extraDur = extraDur + itemDur;
			} {
				first = item.children.detect { |ch| ch.isSpacer.not };
				if(first.notNil) {
					extraDur = first.time - itemOnset;
				} {
					extraDur = extraDur + itemDur;
				}
			};
			i = i - 1;
		};
		currentEnvironment
	};
} => PR(\clPatStringNode);

PR(\abstractClParseNode).clone {
	~isPitch = false;
	~parse = { |stream|
		var str = String.new, ch;
		// stream.collection[stream.pos .. stream.pos + 10].debug(">> clPatStringNode");
		if(stream.peek != $[) {
			Error("Invalid bracketed source string").throw;
		};
		while {
			ch = stream.next;
			ch.notNil and: { ch != $] }
		} {
			~children = ~children.add(PR(\clDividerNode).copy
				.putAll((bpKey: ~bpKey, isPitch: ~isPitch, isMain: ~isMain, parm: ~parm, endChars: "|]"))
				.prep(stream, currentEnvironment)
			);
		};
		if(ch != $]) {
			Error("Unterminated bracketed source string").throw;
		};
		// stream.collection[~begin .. ~begin + 10].debug("<< clPatStringNode");
	};
	~streamCode = { |stream|
		var needComma = false;
		~setTime.(~time, ~dur);
		stream << "PR(\\clGenSrc).copy.putAll((";
		stream << "bpKey: " <<< ~bpKey;
		stream << ", args: [ [ ";
		~children.do { |child, i|
			if(child.hasItems) /*{
				stream << "nil"
			}*/ {
				if(needComma) { stream << ", " };
				child.streamCode(stream);
				needComma = true;
			};
		};
		stream << " ] ], dur: " << ~dur << ", time: " << ~time;
		stream << ", isPitch: " << (~isPitch ?? { false });
		stream << ", isMain: " << (~isMain ?? { false });
		stream << ", parm: " <<< ~parm;
		stream << ")).prep";
		currentEnvironment
	};
	~setTime = PR(\clPatStringNode).v[\setTime];
} => PR(\clSourceNode);

PR(\abstractClParseNode).clone {
	~clClass = BP;
	~objExists = false;
	~phrase = \main;
	~parm = nil;  // filled in in 'parse'

	~idAllowed = "_*";
	~parse = { |stream|
		var i, ids, test, sym, temp, broke;

		broke = block { |break|
			// class (I expect this won't be used often)
			test = ~getID.(stream);
			~children = ~children.add(test);
			if(test.string.first.isUpper) {
				~clClass = test.symbol.asClass;
				if(stream.next != $.) { break.(true) };
				test = ~getID.(stream);
				~children = ~children.add(test);
			};

			// chucklib object key
			~objKey = test.symbol;  // really? what about array types?
			if(~clClass.exists(~objKey)) {
				~objExists = true;
			};
			if(stream.next != $.) { break.(true) };
			test = ~getID.(stream);
			~children = ~children.add(test);

			// phrase name
			test = test.string;
			if(test.size == 0) {
				~phrase = \main;
			} {
				i = test.indexOf($*);
				if(i.notNil) {
					temp = test[i+1 .. ];
					if(temp.notEmpty and: temp.every(_.isDecDigit)) {
						~numToApply = temp.asInteger;
					} {
						"%: Invalid apply number".format(test).warn;
					};
					~phrase = test[ .. i-1].asSymbol;
				} {
					~phrase = test.asSymbol;  // really? what about array types?
				};
			};
			if(stream.next != $.) { break.(true) };
			test = ~getID.(stream);
			~children = ~children.add(test);

			// parameter name
			~parm = test.string;
			false;
		};
		if(~parm.size == 0) {
			if(~objExists) {
				~parm = ~clClass.new(~objKey)[\defaultParm] ?? { \main };
			};
		} {
			~parm = ~parm.asSymbol;
		};
		if(broke) { stream.pos = stream.pos - 1 };
		currentEnvironment
	};

	~streamCode = { |stream|
		"clIDNode:streamCode not yet implemented".warn;
		stream << "IDNode";
		currentEnvironment
	};
} => PR(\clIDNode);

PR(\abstractClParseNode).clone {
	~parse = { |stream|
		var str = String.new, ch;
		while {
			ch = stream.next;
			ch.notNil and: { ch != $" }
		} {
			str = str.add(ch);
		};
		if(ch.notNil) { stream.pos = stream.pos - 1 };
		~string = str;
		~additiveRhythm = str[0] == $+;
		~quant = str[~additiveRhythm.asInteger ..].interpret;
	};
} => PR(\clPatStringQuantNode);

PR(\abstractClParseNode).clone {
	~isPitch = false;
	~hasQuant = false;
	// getters, because ~children positions may vary
	~idNode = { ~children[0] };
	~quantNode = {
		if(~hasQuant) { ~children[1] } { nil };
	};
	~patStringNode = {
		if(~hasQuant) { ~children[2] } { ~children[1] };
	};
	// caller needs to wrap the outermost patStringNode in a generatorNode: need setter
	~patStringNode_ = { |node|
		if(~hasQuant) {
			~children[2] = node;
		} {
			~children[1] = node;
		};
		currentEnvironment
	};
	~parse = { |stream|
		var id, obj;
		if(stream.peek == $/) { stream.next };
		id = PR(\clIDNode).copy.prep(stream, currentEnvironment);
		~children = ~children.add(id);
		~skipSpaces.(stream);
		if(stream.peek == $=) {
			stream.next;
		} {
			Error("clPatternSet must have '='").throw;
		};
		~skipSpaces.(stream);
		if(stream.peek == $() {
			Error("Composite patterns not refactored yet").throw;
		};
		if(stream.peek.isDecDigit or: { stream.peek == $+ }) {
			~children = ~children.add(PR(\clPatStringQuantNode).copy.prep(stream));
			~hasQuant = true;
		};
		if(stream.peek == $\") {
			if(id.clClass.exists(id.objKey)) {
				obj = id.clClass.new(id.objKey);
				try {
					~isPitch = obj.parmIsPitch(id.parm) ?? { false };
					~isMain = id.parm == obj.defaultParm ?? { false };
				};
			};
			~children = ~children.add(
				PR(\clPatStringNode).copy
				.putAll((bpKey: id.objKey, isPitch: ~isPitch, isMain: ~isMain, parm: id.parm))
				.prep(stream, currentEnvironment)
			);
		}
	};

	// ~setPattern = { |phrase, parm, inParm, pattern, inString, newQuant| };

	// maybe caller should be responsible for this
	~streamCode = { |stream|
		// var id = ~children[0];
		// stream << id.clClass.name << "(" <<< id.name << ").setPattern(";
		// stream <<< id.phrase << ", "
		currentEnvironment
	};
	~setTime = { |onset(0), dur(4)|
		~time = onset;
		~dur = dur;
		~children[1].setTime(onset, dur);  // [1] is the patstring
		currentEnvironment
	};
} => PR(\clPatternSetNode);



// set nodeType tags
current = thisProcess.nowExecutingPath.basename;
PR.all.do { |pr|
	if(pr.path.tryPerform(\basename) == current) {
		pr.v[\nodeType] = pr.collIndex;
	};
};