  const body = document.querySelector("body"),
  sidebar = body.querySelector("nav"),
  toggle = body.querySelector("#toggle"),
  modeSwitch = body.querySelector(".toggle-switch"),
  modeText = body.querySelector(".mode-text");
toggle.addEventListener("click", () => {
  unHideElement("closebtn");
  hideElement("toggle");
  sidebar.classList.toggle("close");
});

function closeNav()
{
hideElement("closebtn");
unHideElement("toggle");
sidebar.classList.toggle('close');
}