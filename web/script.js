function calculateHash(event) {
    event.preventDefault()

    let formData = new FormData(submitForm)

    let algorithm = formData.get("algorithm")
    for (value of formData.getAll("files")) {
        let fileReader = new FileReader()
        fileReader.readAsBinaryString(value)
        fileReader.onloadend = function () {
            if (algorithm === "md5") {
                console.log(CryptoJS.SHA256(CryptoJS.enc.Latin1.parse(fileReader.result)).toString())
            } else if (algorithm === "sha1") {
                console.log(CryptoJS.SHA1(CryptoJS.enc.Latin1.parse(fileReader.result)).toString())
            } else if (algorithm === "sha256") {
                console.log(CryptoJS.SHA256(CryptoJS.enc.Latin1.parse(fileReader.result)).toString())
            }
        }
    }
}

const submitForm = document.getElementById("submit_form")
submitForm.addEventListener("submit", calculateHash)