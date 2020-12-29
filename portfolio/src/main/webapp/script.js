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
 * Adds in navigation bar to each page.
 */
document.addEventListener('DOMContentLoaded', loadNavBar);
function loadNavBar(){ 
    document.getElementById("nav-bar").innerHTML='<object type="text/html" data="nav-bar.html" width=100% height="50"></object >';
}

/**
 * Adds a random greeting to the welcome home page.
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

/**
 * Adds a random tv quote to the welcome home page.
 */
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

/**
 * Load comments from server and add them to page.
 */
async function loadComments() {

  // Add users limit to query string
  const userLimit = document.getElementById("limit").value;
  dataURL = "/data?limit=" + userLimit;
  
  const response = await fetch(dataURL);
  const comments = await response.json();

  const commentListElement = document.getElementById('comment-container');
  commentListElement.innerHTML = "";
  comments.forEach((comment) => {
    commentListElement.appendChild(createCommentElement(comment));
  })

}

/** 
 * Creates an <li> element for a comment that has name,comment, time and option to delete. 
 */
function createCommentElement(comment) {

  const commentElement = document.createElement('li');
  commentElement.setAttribute("class", "comment");
  commentElement.className = 'comment';

  const nameElement = document.createElement('span');
  nameElement.setAttribute("class", "comment-name");
  nameElement.innerText = "Name:" + comment.name + "    ";

  const messageElement = document.createElement('span');
  messageElement.setAttribute("class", "comment-message");
  messageElement.innerText = "Comment:" + comment.message + "    ";

  const timeElement = document.createElement('span');
  timeElement.setAttribute("class", "comment-time");
  timeElement.innerText = "Time:" + comment.timestamp;

  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.setAttribute("class", "comment-delete");
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteComment(comment);

    // Remove the task from the DOM.
    commentElement.remove();
  });
  
  commentElement.appendChild(nameElement);
  commentElement.appendChild(messageElement);
  commentElement.appendChild(timeElement);
  commentElement.appendChild(deleteButtonElement);
  return commentElement;
}

/** Tells the server to delete the comment. */
function deleteComment(comment) {
  const params = new URLSearchParams();
  params.append('id', comment.id);
  fetch('/delete-data', {method: 'POST', body: params});
}

/** Tells the server to delete ALL comments */
async function deleteAllComments(){

  // Get all the comments on the server.
  const response = await fetch("/data");
  const comments = await response.json();

  // Delete each comment from the server.
  var i;
  for(i=0; i< comments.length; i++){
    deleteComment(comments[i]);
  }

  // Update so no more comments i.e. empty.
  const commentListElement = document.getElementById('comment-container');
  commentListElement.innerHTML = "";

}

/** 
 * Tells the server to translate comments to specified language
 */
async function requestTranslation() {

  // Get language to translate to
  const languageCode = document.getElementById('language').value;

  // Select the comment (name,message,time) and translate

  const commentNames = document.querySelectorAll(".comment-name");
    commentNames.forEach(name => {
      translate(name, languageCode)
    });  

  const commentMessages = document.querySelectorAll(".comment-message");
    commentMessages.forEach(message => {
      translate(message, languageCode)
    });  

  const commentTime = document.querySelectorAll(".comment-time");
    commentTime.forEach(time => {
      translate(time, languageCode)
    });
}

function translate(textElement,languageCode) {

  const translatedComment = textElement;

  const params = new URLSearchParams();
  params.append('comment', textElement.textContent);
  params.append('languageCode', languageCode);

  // Translate and update text to different language.
  fetch('/translate', {
    method: 'POST',
    body: params
  }).then(response => response.text())
  .then((translatedMessage) => {
    translatedComment.innerText = translatedMessage;
  });
}
