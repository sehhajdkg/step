// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * Adds a random greeting to the page.
 */
function addRandomGreeting() {
  const greetings =
      ['Hello world!', '¡Hola Mundo!', '你好，世界！', 'Bonjour le monde!'];

  // Pick a random greeting.
  const greeting = greetings[Math.floor(Math.random() * greetings.length)];

  // Add it to the page.
  const greetingContainer = document.getElementById('greeting-container');
  greetingContainer.innerText = greeting;
}

function addRandomTVQuote() {
  const tvQuotes =
      ['"Great, Get In There And Operate, Doctor Google" - Kendall Roy [Succession]', 
      '“Sometimes I’ll start a sentence and I don’t even know where it’s going. I just hope I find it along the way." – Michael Scott [The Office]', 
      '"If Anyone Is Feeling Anxious Or Worried Or Even If You Just Want To Chat, Please, Please, Do Not Come Crying To Me.” - Sister Michael [Derry Girls]', 
      '“You should wash your hands, you dirty pig! - Eric Effiong [Sex Education]', 
      '“I Get It Now, Why Men Rule The World: No High Heels.” - Midge Maisel [The Marvelous Mrs Maisel]', 
      '"Huzzah" - Peter III of Russia [The Great]'];

  // Pick a random quote.
  const tvQuote = tvQuotes[Math.floor(Math.random() * tvQuotes.length)];

  // Add it to the page.
  const tvQuoteContainer = document.getElementById('tvQuote-container');
  tvQuoteContainer.innerText = tvQuote;
}

