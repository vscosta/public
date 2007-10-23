/*
This is not my own work. I found it in

CMPT 384 Lecture Notes
Robert D. Cameron
November 29 - December 1, 1999

http://www.cs.sfu.ca/people/Faculty/cameron/Teaching/384/99-3/regexp-plg.html

Hope it is ok to reuse this. If you think it isn't, please let me know.

--lu

*/
:- module(pdt_regex,[pdt_regex_match/4,pdt_regex_match1/4,pdt_regex/2]).

re(Z) --> basicRE(W), reTail(W, Z).
reTail(W, Z) --> "|", basicRE(X), reTail(union(W,X), Z).
reTail(W, W) --> {true}.
basicRE(Z) --> simpleRE(W), basicREtail(W, Z).
basicREtail(W, Z) --> simpleRE(X), basicREtail(conc(W,X), Z).
basicREtail(W, W) --> {true}.
simpleRE(Z) --> elementalRE(W), simpleREtail(W, Z).
simpleREtail(W, star(W)) --> "*".
simpleREtail(W, plus(W)) --> "+".
simpleREtail(W, W) --> {true}.
elementalRE(any) --> ".".
elementalRE(group(X)) --> "(", re(X), ")".
elementalRE(eos) --> "$".
elementalRE(char(C)) --> [C], {\+(re_metachar([C]))}.
re_metachar("\\").
re_metachar("\|").
re_metachar("*").
re_metachar("+").
re_metachar("\.").
re_metachar("[").
re_metachar("$").
re_metachar("(").
re_metachar(")").
elementalRE(char(C)) --> "\\", [C], {re_metachar([C])}.
%  For sets, first try the negative set syntax.  If the "[^" recognition
%  succeeds, use cut to make sure that any subsequent failure does not
%  cause the positive set interpretation to be used.
elementalRE(negSet(X)) --> "[^", {!}, setItems(X), "]".
elementalRE(posSet(X)) --> "[", setItems(X), "]".
setItems([Item1|MoreItems]) --> setItem(Item1), setItems(MoreItems).
setItems([Item1]) --> setItem(Item1).
setItem(char(C)) --> [C], {\+(set_metachar([C]))}.
set_metachar("\\").
set_metachar("]").
set_metachar("-").
setItem(char(C)) --> "\\", [C], {set_metachar([C])}.
setItem(range(A,B)) --> setItem(char(A)), "-", setItem(char(B)).

:- dynamic '$cached'/2.

pdt_regex(Input,RE):-
    '$cached'(Input,RE),
    !.
pdt_regex(Input,RE):-   
	(	atom(Input)
	->	atom_codes(Input,Codes)
	;	Input=Codes
	), 
    re(RE,Codes,[]),
    assert('$cached'(Input,RE)).

pdt_regex_match(Pattern,S,Unmatched,Selected):-
    pdt_regex(Pattern,RE),
    rematch1(RE,S,Unmatched,Selected).
    
pdt_regex_match1(RE,S,Unmatched,Selected):-
    rematch1(RE,S,Unmatched,Selected).
%
% rematch1(RE, S, Unmatched, Selected) is true if RE matches
% a string Prefix such that S = [Prefix|Unmatched], and
% Selected is the list of substrings of Prefix that matched
% the parenthesized components of RE.

rematch1(union(RE1, _RE2), S, U, Selected) :- 
  rematch1(RE1, S, U, Selected).
rematch1(union(_RE1, RE2), S, U, Selected) :- 
  rematch1(RE2, S, U, Selected).
rematch1(conc(RE1, RE2), S, U, Selected) :- 
  rematch1(RE1, S, U1, Sel1),
  rematch1(RE2, U1, U, Sel2),
  append(Sel1, Sel2, Selected).
% Try longest match first.
rematch1(star(RE), S, U, Selected) :-
  rematch1(RE, S, U1, Sel1),
  rematch1(star(RE), U1, U, Sel2),
  append(Sel1, Sel2, Selected).
rematch1(star(_RE), S, S, []).
rematch1(plus(RE), S, U, Selected) :-
  rematch1(RE, S, U1, Sel1),
  rematch1(star(RE), U1, U, Sel2),
  append(Sel1, Sel2, Selected).
% Match a group and add it to the end of
% list of selected items from the submatch.
rematch1(group(RE), S, U, Selected) :-
  rematch1(RE, S, U, Sel1),
  append(P, U, S),
  append(Sel1, [P], Selected).

rematch1(any, [_C1|U], U, []).
% Note that the following works for matching both regular
% characters and metacharacters.  
rematch1(char(C), [C|U], U, []).

rematch1(eos, [], [], []).

rematch1(negSet(Set), [C|U], U, []) :-
  \+(charSetMember(C, Set)).

rematch1(posSet(Set), [C|U], U, []) :-
  charSetMember(C, Set).

charSetMember(C, [char(C) | _]).
charSetMember(C, [range(C1, C2) | _]) :-
  C1 =< C,
  C =< C2.
charSetMember(C, [_|T]) :- charSetMember(C, T).
