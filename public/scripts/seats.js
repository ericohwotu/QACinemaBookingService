let host = "";
let callback;


window.onload = function () {
    host = window.location.hostname;
    popDates();
    refresh();
}


function selectSeat(seatId) {
    console.log("select seat called");
    let xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState == 4 && this.status == 200) {
            changeSeatColor(document.getElementById("seat-" + seatId), JSON.parse(this.response));
        }
    };
    xhttp.open("POST", "http://" + host + ":9000/seats/json?id=" + seatId
        + "&date=" + getSelectedText("days") + "&time=" + getSelectedText("times"), true);
    xhttp.send();
}

function isSeatLimitReached(){
    console.log("is seat reached")
    let bookedCount = document.getElementsByClassName("booked").length
    let submitBooking = document.getElementById("submit-booking")

    let total = getStandardTicketCount() + getVipTicketCount();

    console.log("===============================================================")
    console.log(total)
    console.log("===============================================================")

    if(bookedCount != total || total == 0)
        submitBooking.setAttribute("disabled", "true");
    else
        submitBooking.removeAttribute("disabled");

    return bookedCount >= total
}

function isStandardLimitReached(){
    console.log("is standard reached called")

    let bookedCount = document.getElementsByClassName("standard booked").length;


    isSeatLimitReached()
    return bookedCount >= getStandardTicketCount()

}

function isVipLimitReached(){
    console.log("is vip reached called");
    let bookedCount = document.getElementsByClassName("vip booked").length;
    console.log("isVipLi")
    console.log(bookedCount)

    isSeatLimitReached()
    return bookedCount >= getVipTicketCount()
}

function getStandardTicketCount(){
    console.log("get standard tickets count called");
    let sAdult = +document.getElementById("standard-adult").value;
    let sStudent = +document.getElementById("standard-student").value;
    let sChild = +document.getElementById("standard-child").value;

    return sAdult + sStudent + sChild
}

function getVipTicketCount(){
    console.log("get vip tickets count called");
    let vAdult = +document.getElementById("vip-adult").value;
    let vStudent = +document.getElementById("vip-student").value;
    let vChild = +document.getElementById("vip-child").value;

    return vAdult + vStudent + vChild
}

function changeSeatColor(elem, json) {
    console.log("change seat color called");
    elem.classList.remove("available");
    elem.classList.remove("booked");

    console.log(json)
    if (json.outcome === "failure")
        elem.classList.add("unavailable")
    else if (json.outcome === "success" && json.message === "seat booked")
        elem.classList.add("booked")
    else
        elem.classList.add("available")

    if(isStandardLimitReached())disableStandard()
    else enableStandard();
    if(isVipLimitReached())disableVip()
    else enableVip();
}

function refresh() {
    console.log("refressh called")
    //get an ajax call to book the seat
    let xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState == 4 && this.status == 200) {
            updateButtons(JSON.parse(this.response));
        }
    };
    xhttp.open("GET", "http://" + host + ":9000/seats/json?date=" +
        getSelectedText("days") + "&time=" + getSelectedText("times"), true);
    xhttp.send();
}

function getSelectedText(elementId) {
    let elem = document.getElementById(elementId);

    if (elem.selectedIndex == -1)
        return null;

    return elem.options[elem.selectedIndex].text;
}

function updateButtons(arr) {
    console.log("update buttons called")
    for (let i = 0; i < arr.length; i++)
        updateButton(arr[i]);

    if(isStandardLimitReached())disableStandard()
    if(isVipLimitReached())disableVip()

}

function updateButton(json) {
    console.log("update button called");
    let elem = document.getElementById("seat-" + json.seatid);
    elem.classList.remove("standard");
    elem.classList.add("standard");
    elem.classList.remove("available");
    elem.classList.remove("booked");
    elem.classList.remove("unavailable");



    if (json.available === "true")
        elem.classList.add("available");
    else if (json.available === "false" && json.bookedBy === "true")
        elem.classList.add("booked");
    else {
        elem.classList.add("unavailable");
        elem.setAttribute("disabled", "true");
    }

    if (json.type == "VIP")setVipButton(elem);

    if (json.type == "EMPTY"){
        elem.classList.remove("standard");
        elem.classList.add("empty");
        elem.setAttribute("disabled", "true");
    }
}

function setVipButton(seat){
    console.log("set vip button");
    seat.classList.remove("standard")
    seat.classList.remove("vip")
    seat.classList.add("vip")
}

function enableStandard() {
    console.log("enable Standadd called");
    clearInterval(callback);
    callback = setInterval(refresh, 5000);
    let elems = document.getElementsByClassName("standard");

    for (let i = 0; i < elems.length; i++) {
        elems[i].removeAttribute("disabled");
    }

    refresh();
}

function enableVip() {
    console.log("eableVip called")
    clearInterval(callback);
    callback = setInterval(refresh, 5000);
    let elems = document.getElementsByClassName("vip");

    for (let i = 0; i < elems.length; i++) {
        elems[i].removeAttribute("disabled");
    }

    refresh();
}

function disableStandard() {
    console.log("disableStandard called")
    let elems = document.getElementsByClassName("standard");

    for (let i = 0; i < elems.length; i++) {
        if(!elems[i].classList.contains("booked")) {
            elems[i].setAttribute("disabled", "true");
            elems[i].classList.remove("available");
            elems[i].classList.add("unavailable");
        }
    }
    clearInterval(callback);
}

function disableVip(){
    console.log("disableVip called")
    let elems = document.getElementsByClassName("vip");

    for (let i = 0; i < elems.length; i++) {
        if(!elems[i].classList.contains("booked")) {
            elems[i].setAttribute("disabled", "true");
            elems[i].classList.remove("available");
            elems[i].classList.add("unavailable");
        }
    }
    clearInterval(callback);
}

